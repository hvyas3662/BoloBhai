package com.bolobhai.data

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import kotlin.io.path.createDirectories

/** One processed output of a dictation; shown as v1, v2… (BoloBhai.md §15). */
data class Version(
    val id: Long,
    val idx: Int,
    val profileId: String?,
    val profileName: String,
    val output: String,
    val llmModel: String?,
    val latencyMs: Long?,
    val createdAt: Long,
)

/** A version to append; its idx and id are assigned by the repo. */
data class NewVersion(
    val profileId: String?,
    val profileName: String,
    val output: String,
    val llmModel: String?,
    val latencyMs: Long?,
)

/** A dictation with all its versions (BoloBhai.md §15). */
data class Dictation(
    val id: Long,
    val createdAt: Long,
    val durationMs: Long,
    val rawTranscript: String,
    val audioPath: String?,
    val status: String, // ok | llm_failed | stt_failed
    val currentVersion: Int,
    val language: String,
    val whisperModel: String,
    val versions: List<Version>,
) {
    val current: Version? get() = versions.getOrNull(currentVersion)
}

/**
 * History store: SQLite (xerial) with an FTS5 index over raw transcript + all version outputs
 * (BoloBhai.md §13.1, §15). One Connection kept open for the app's lifetime.
 *
 * shortcut: FTS table is a plain fts5(raw, outputs) keyed by rowid=history.id, not the spec's
 * content='' external-content form — identical search behaviour, far simpler to keep in sync.
 */
class HistoryRepo(dbPath: Path) {
    private val conn: Connection

    init {
        dbPath.parent?.createDirectories()
        conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        conn.createStatement().use { it.execute("PRAGMA foreign_keys=ON") }
        createSchema()
    }

    private fun createSchema() = conn.createStatement().use { st ->
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS history (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              created_at INTEGER NOT NULL,
              duration_ms INTEGER NOT NULL,
              raw_transcript TEXT NOT NULL,
              audio_path TEXT,
              status TEXT NOT NULL CHECK (status IN ('ok','llm_failed','stt_failed')),
              current_version INTEGER NOT NULL DEFAULT 0,
              language TEXT NOT NULL,
              whisper_model TEXT NOT NULL,
              profile_name TEXT,
              final_output TEXT
            )
            """.trimIndent()
        )
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS history_version (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              history_id INTEGER NOT NULL REFERENCES history(id) ON DELETE CASCADE,
              idx INTEGER NOT NULL,
              profile_id TEXT,
              profile_name TEXT NOT NULL,
              output TEXT NOT NULL,
              llm_model TEXT,
              latency_ms INTEGER,
              created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        st.executeUpdate("CREATE VIRTUAL TABLE IF NOT EXISTS history_fts USING fts5(raw, outputs)")
        st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_history_created ON history(created_at DESC)")
    }

    fun insert(
        createdAt: Long,
        durationMs: Long,
        rawTranscript: String,
        audioPath: String?,
        status: String,
        language: String,
        whisperModel: String,
        version: NewVersion,
    ): Dictation {
        conn.prepareStatement(
            """
            INSERT INTO history
              (created_at, duration_ms, raw_transcript, audio_path, status,
               current_version, language, whisper_model, profile_name, final_output)
            VALUES (?,?,?,?,?,0,?,?,?,?)
            """.trimIndent(),
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, createdAt)
            ps.setLong(2, durationMs)
            ps.setString(3, rawTranscript)
            ps.setString(4, audioPath)
            ps.setString(5, status)
            ps.setString(6, language)
            ps.setString(7, whisperModel)
            ps.setString(8, version.profileName)
            ps.setString(9, version.output)
            ps.executeUpdate()
            val id = ps.generatedKeys.use { it.next(); it.getLong(1) }
            insertVersion(id, 0, version)
            syncFts(id)
            return get(id)!!
        }
    }

    /** Append a new version and make it current (BoloBhai.md §13.1 re-process). */
    fun addVersion(historyId: Long, version: NewVersion): Dictation {
        val nextIdx = nextIdx(historyId)
        insertVersion(historyId, nextIdx, version)
        conn.prepareStatement(
            "UPDATE history SET current_version=?, profile_name=?, final_output=? WHERE id=?"
        ).use { ps ->
            ps.setInt(1, nextIdx)
            ps.setString(2, version.profileName)
            ps.setString(3, version.output)
            ps.setLong(4, historyId)
            ps.executeUpdate()
        }
        syncFts(historyId)
        return get(historyId)!!
    }

    fun list(): List<Dictation> = query("SELECT * FROM history ORDER BY created_at DESC, id DESC")

    fun search(queryText: String): List<Dictation> {
        val match = toMatch(queryText) ?: return emptyList()
        conn.prepareStatement(
            """
            SELECT history.* FROM history
            JOIN history_fts ON history_fts.rowid = history.id
            WHERE history_fts MATCH ?
            ORDER BY history.created_at DESC, history.id DESC
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, match)
            return readDictations(ps)
        }
    }

    fun get(id: Long): Dictation? {
        conn.prepareStatement("SELECT * FROM history WHERE id=?").use { ps ->
            ps.setLong(1, id)
            return readDictations(ps).firstOrNull()
        }
    }

    /** Delete an item (versions cascade) and return its audio path for file cleanup. */
    fun delete(id: Long): String? {
        val audio = get(id)?.audioPath
        conn.prepareStatement("DELETE FROM history WHERE id=?").use { it.setLong(1, id); it.executeUpdate() }
        conn.prepareStatement("DELETE FROM history_fts WHERE rowid=?").use { it.setLong(1, id); it.executeUpdate() }
        return audio
    }

    /** Delete everything; return every stored audio path for file cleanup. */
    fun clearAll(): List<String> {
        val paths = list().mapNotNull { it.audioPath }
        conn.createStatement().use { st ->
            st.executeUpdate("DELETE FROM history")
            st.executeUpdate("DELETE FROM history_fts")
        }
        return paths
    }

    /** Null out audio paths older than [cutoffMs]; return the files to delete (§Settings keepAudio). */
    fun clearAudioOlderThan(cutoffMs: Long): List<String> {
        val stale = list().filter { it.createdAt < cutoffMs && it.audioPath != null }
        conn.prepareStatement("UPDATE history SET audio_path=NULL WHERE created_at<? AND audio_path IS NOT NULL")
            .use { it.setLong(1, cutoffMs); it.executeUpdate() }
        return stale.mapNotNull { it.audioPath }
    }

    fun count(): Int = conn.createStatement().use { st ->
        st.executeQuery("SELECT COUNT(*) FROM history").use { it.next(); it.getInt(1) }
    }

    fun close() = conn.close()

    // --- internals ---

    private fun insertVersion(historyId: Long, idx: Int, v: NewVersion) {
        conn.prepareStatement(
            """
            INSERT INTO history_version
              (history_id, idx, profile_id, profile_name, output, llm_model, latency_ms, created_at)
            VALUES (?,?,?,?,?,?,?,?)
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, historyId)
            ps.setInt(2, idx)
            ps.setString(3, v.profileId)
            ps.setString(4, v.profileName)
            ps.setString(5, v.output)
            ps.setString(6, v.llmModel)
            if (v.latencyMs == null) ps.setNull(7, java.sql.Types.INTEGER) else ps.setLong(7, v.latencyMs)
            ps.setLong(8, System.currentTimeMillis())
            ps.executeUpdate()
        }
    }

    private fun nextIdx(historyId: Long): Int =
        conn.prepareStatement("SELECT COALESCE(MAX(idx)+1,0) FROM history_version WHERE history_id=?").use { ps ->
            ps.setLong(1, historyId)
            ps.executeQuery().use { it.next(); it.getInt(1) }
        }

    /** Rebuild the FTS row for an item: raw transcript + every version's output. */
    private fun syncFts(historyId: Long) {
        val item = get(historyId) ?: return
        val outputs = item.versions.joinToString("\n") { it.output }
        conn.prepareStatement("DELETE FROM history_fts WHERE rowid=?").use { it.setLong(1, historyId); it.executeUpdate() }
        conn.prepareStatement("INSERT INTO history_fts(rowid, raw, outputs) VALUES (?,?,?)").use { ps ->
            ps.setLong(1, historyId)
            ps.setString(2, item.rawTranscript)
            ps.setString(3, outputs)
            ps.executeUpdate()
        }
    }

    private fun query(sql: String): List<Dictation> = conn.prepareStatement(sql).use { readDictations(it) }

    private fun readDictations(ps: java.sql.PreparedStatement): List<Dictation> {
        val items = mutableListOf<Dictation>()
        ps.executeQuery().use { rs ->
            while (rs.next()) {
                val id = rs.getLong("id")
                items += Dictation(
                    id = id,
                    createdAt = rs.getLong("created_at"),
                    durationMs = rs.getLong("duration_ms"),
                    rawTranscript = rs.getString("raw_transcript"),
                    audioPath = rs.getString("audio_path"),
                    status = rs.getString("status"),
                    currentVersion = rs.getInt("current_version"),
                    language = rs.getString("language"),
                    whisperModel = rs.getString("whisper_model"),
                    versions = versionsOf(id),
                )
            }
        }
        return items
    }

    private fun versionsOf(historyId: Long): List<Version> {
        conn.prepareStatement("SELECT * FROM history_version WHERE history_id=? ORDER BY idx").use { ps ->
            ps.setLong(1, historyId)
            ps.executeQuery().use { rs ->
                val out = mutableListOf<Version>()
                while (rs.next()) {
                    out += Version(
                        id = rs.getLong("id"),
                        idx = rs.getInt("idx"),
                        profileId = rs.getString("profile_id"),
                        profileName = rs.getString("profile_name"),
                        output = rs.getString("output"),
                        llmModel = rs.getString("llm_model"),
                        latencyMs = rs.getLong("latency_ms").let { if (rs.wasNull()) null else it },
                        createdAt = rs.getLong("created_at"),
                    )
                }
                return out
            }
        }
    }

    /** Prefix match, case-insensitive: each token gets a trailing `*`. Null when blank. */
    private fun toMatch(queryText: String): String? {
        val tokens = queryText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"" + it.replace("\"", "\"\"") + "\"*" }
    }
}
