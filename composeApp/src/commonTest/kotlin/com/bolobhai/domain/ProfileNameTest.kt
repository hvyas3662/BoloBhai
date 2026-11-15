package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileNameTest {

    @Test
    fun knownDefaults_mapToSpecNames() {
        assertEquals("Hinglish", profileDisplayName("hinglish.md"))
        assertEquals("Proper English", profileDisplayName("proper-english.md"))
        assertEquals("AI Prompt", profileDisplayName("ai-prompt.md"))
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
