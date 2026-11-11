package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileParseTest {

    @Test
    fun parsesFrontmatterNameAndBody() {
        val content = """
            ---
            name: Hinglish Notes
            ---
            Write everything in Roman letters.
            Keep it short.
        """.trimIndent()
        val p = parseProfile("hinglish-clean.md", content)
        assertEquals("Hinglish Notes", p.name)
        assertEquals("Write everything in Roman letters.\nKeep it short.", p.body)
        assertNull(p.emoji)
    }

    @Test
    fun readsEmojiButBodyExcludesFrontmatter() {
        val content = "---\nname: Email\nemoji: 📧\n---\nTurn into an email."
        val p = parseProfile("email.md", content)
        assertEquals("Email", p.name)
        assertEquals("📧", p.emoji)
        assertEquals("Turn into an email.", p.body)
    }

    @Test
    fun noFrontmatter_derivesNameFromFile_bodyIsWhole() {
        val p = parseProfile("my-custom.md", "Just a prompt.")
        assertEquals("My Custom", p.name)
        assertEquals("Just a prompt.", p.body)
    }

    @Test
    fun frontmatterWithoutName_derivesFromFile() {
        val p = parseProfile("whatsapp-message.md", "---\nemoji: 💬\n---\nCasual message.")
        assertEquals("WhatsApp Message", p.name)
        assertEquals("💬", p.emoji)
    }

    @Test
    fun serializeRoundTrips() {
        val p = Profile("email.md", "My Email", "Body line 1\nLine 2", emoji = "📧")
        val reparsed = parseProfile("email.md", serializeProfile(p))
        assertEquals(p.name, reparsed.name)
        assertEquals(p.body, reparsed.body)
        assertEquals(p.emoji, reparsed.emoji)
    }

    @Test
    fun slug_normalizes() {
        assertEquals("hinglish-notes", slug("Hinglish Notes"))
        assertEquals("email", slug("Email!!"))
        assertEquals("my-profile", slug("  My  Profile  "))
        assertEquals("numbered-list", slug("Numbered List"))
    }
}
