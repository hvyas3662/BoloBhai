package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemPromptTest {
    @Test
    fun hinglishRomanizedFillsPlaceholdersAndExample() {
        val p = renderBaseSystemPrompt("hi-en", "romanized")
        assertFalse(p.contains("{{"), "no placeholders should remain: $p")
        assertContains(p, "Hindi and English")
        assertContains(p, "Latin (English) letters, including Hindi words")
        assertContains(p, "Never output Devanagari")
        assertContains(p, "kal meeting hai")
    }

    @Test
    fun nativeScriptUsesStandardScriptWording() {
        val p = renderBaseSystemPrompt("hi-en", "native")
        assertContains(p, "standard script")
        assertFalse(p.contains("kal meeting hai"))
    }

    @Test
    fun allLatinLanguageOmitsNativeScriptClause() {
        val p = renderBaseSystemPrompt("es-en", "romanized")
        assertContains(p, "Spanish and English")
        assertFalse(p.contains("Never output"), "Spanish+English has no non-Latin script clause")
    }

    @Test
    fun autoModeUsesDetectPhrase() {
        val p = renderBaseSystemPrompt("auto", "native")
        assertContains(p, "the language(s) that make up most of the transcript")
    }

    @Test
    fun unknownKeysFallBackToDefaults() {
        assertEquals("hi-en", languageOptionOf("nonsense").key)
        assertEquals(ScriptRule.Romanized, scriptRuleOf("nonsense"))
    }

    @Test
    fun buildStacksProfileBodyOnTop() {
        val built = buildSystemPrompt("Turn it into a list.", "hi-en", "romanized")
        assertTrue(built.trimEnd().endsWith("Turn it into a list."))
        assertContains(built, "You clean up dictated speech-to-text transcripts.")
    }
}
