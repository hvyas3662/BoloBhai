package com.bolobhai.data

import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryRepoTest {
    private lateinit var dir: java.nio.file.Path
    private lateinit var repo: HistoryRepo

    @BeforeTest
    fun setup() {
        dir = Files.createTempDirectory("history-test")
        repo = HistoryRepo(dir.resolve("history.db"))
    }

    @AfterTest
    fun tearDown() {
        repo.close()
        dir.toFile().deleteRecursively()
    }

    private fun insert(
        raw: String = "mera naam kya hai",
        output: String = "Mera naam kya hai",
        profileId: String? = "hinglish-clean.md",
        profileName: String = "Hinglish Clean",
        status: String = "ok",
        audioPath: String? = "a.wav",
        createdAt: Long = 1000,
    ) = repo.insert(
        createdAt = createdAt,
        durationMs = 14_000,
        rawTranscript = raw,
        audioPath = audioPath,
        status = status,
        language = "hi",
        whisperModel = "ggml-large-v3-turbo.bin",
        version = NewVersion(profileId, profileName, output, "Qwen3", 900),
    )

    @Test
    fun `insert creates item with version 0 as current`() {
        val item = insert()
        assertTrue(item.id > 0)
        assertEquals(0, item.currentVersion)
        assertEquals(1, item.versions.size)
        assertEquals("Mera naam kya hai", item.current?.output)
        assertEquals("hinglish-clean.md", item.current?.profileId)
    }

    @Test
    fun `list returns newest first`() {
        insert(raw = "first", createdAt = 1000)
        insert(raw = "second", createdAt = 2000)
        val list = repo.list()
        assertEquals(2, list.size)
        assertEquals("second", list[0].rawTranscript)
    }

    @Test
    fun `addVersion appends and becomes current`() {
        val item = insert()
        val updated = repo.addVersion(item.id, NewVersion("email.md", "Email", "Dear sir", "Qwen3", 1200))
        assertEquals(2, updated.versions.size)
        assertEquals(1, updated.currentVersion)
        assertEquals("Email", updated.current?.profileName)
    }

    @Test
    fun `search matches raw and output, prefix, case-insensitive`() {
        insert(raw = "kubernetes cluster", output = "Kubernetes cluster")
        insert(raw = "grocery list", output = "Grocery list")
        assertEquals(1, repo.search("KUBER").size)
        assertEquals(1, repo.search("groc").size)
        assertEquals(0, repo.search("zzz").size)
    }

    @Test
    fun `search finds text added only in a later version`() {
        val item = insert(raw = "raw text", output = "raw text")
        repo.addVersion(item.id, NewVersion("email.md", "Email", "Dear Almirah", null, null))
        assertEquals(1, repo.search("almirah").size)
    }

    @Test
    fun `delete removes item and returns audio path`() {
        val item = insert(audioPath = "keep.wav")
        assertEquals("keep.wav", repo.delete(item.id))
        assertTrue(repo.list().isEmpty())
        assertEquals(0, repo.search("naam").size)
    }

    @Test
    fun `clearAll empties history and returns audio paths`() {
        insert(audioPath = "a.wav")
        insert(audioPath = "b.wav")
        val paths = repo.clearAll()
        assertEquals(setOf("a.wav", "b.wav"), paths.toSet())
        assertTrue(repo.list().isEmpty())
    }

    @Test
    fun `clearAudioOlderThan nulls path and returns files to delete`() {
        val old = insert(audioPath = "old.wav", createdAt = 1000)
        insert(audioPath = "new.wav", createdAt = 9000)
        val removed = repo.clearAudioOlderThan(5000)
        assertEquals(listOf("old.wav"), removed)
        assertNull(repo.get(old.id)?.audioPath)
    }

    @Test
    fun `persists across reopen`() {
        insert(raw = "persist me")
        repo.close()
        repo = HistoryRepo(dir.resolve("history.db"))
        assertEquals("persist me", repo.list().single().rawTranscript)
    }
}
