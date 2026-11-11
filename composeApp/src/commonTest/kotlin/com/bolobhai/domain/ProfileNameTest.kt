package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileNameTest {

    @Test
    fun knownDefaults_mapToSpecNames() {
        assertEquals("Hinglish Clean", profileDisplayName("hinglish-clean.md"))
        assertEquals("Numbered List", profileDisplayName("numbered-list.md"))
        assertEquals("Email", profileDisplayName("email.md"))
        assertEquals("WhatsApp Message", profileDisplayName("whatsapp-message.md"))
        assertEquals("Meeting Notes", profileDisplayName("meeting-notes.md"))
    }

    @Test
    fun unknownFile_isTitleCasedFromSlug() {
        assertEquals("My Custom Prompt", profileDisplayName("my-custom-prompt.md"))
        assertEquals("Foo", profileDisplayName("foo.md"))
    }

    @Test
    fun nullOrBlank_isEmpty() {
        assertEquals("Empty", profileDisplayName(null))
        assertEquals("Empty", profileDisplayName(""))
    }
}
