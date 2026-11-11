package com.bolobhai.domain

/** An instruction profile (BoloBhai.md §11). Profile id = [fileName]. */
data class Profile(
    val fileName: String,
    val name: String,
    val body: String,
    val emoji: String? = null,
)

/**
 * Parse a `.md` profile with optional flat YAML frontmatter (`name:`, `emoji:`). The `name`
 * defaults to a title-cased slug of the file name; the body is everything after the frontmatter.
 */
fun parseProfile(fileName: String, content: String): Profile {
    val normalized = content.replace("\r\n", "\n")
    var name: String? = null
    var emoji: String? = null
    var body = normalized

    if (normalized.startsWith("---\n")) {
        val end = normalized.indexOf("\n---", startIndex = 3)
        if (end != -1) {
            val front = normalized.substring(4, end)
            for (line in front.split("\n")) {
                val colon = line.indexOf(':')
                if (colon <= 0) continue
                val key = line.substring(0, colon).trim()
                val value = line.substring(colon + 1).trim()
                when (key) {
                    "name" -> name = value
                    "emoji" -> emoji = value
                }
            }
            body = normalized.substring(end + 4).removePrefix("\n")
        }
    }

    return Profile(
        fileName = fileName,
        name = name?.takeIf { it.isNotBlank() } ?: profileDisplayName(fileName),
        body = body.trim(),
        emoji = emoji?.takeIf { it.isNotBlank() },
    )
}

/** Serialize a profile back to `.md` with frontmatter. */
fun serializeProfile(profile: Profile): String = buildString {
    append("---\n")
    append("name: ${profile.name}\n")
    if (profile.emoji != null) append("emoji: ${profile.emoji}\n")
    append("---\n")
    append(profile.body)
    append("\n")
}

/** File-name-safe slug of a display name (BoloBhai.md §11: renames to `slug(name).md`). */
fun slug(name: String): String =
    name.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
