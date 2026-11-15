package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemPromptTest {
    @Test
    fun fillsLanguagePlaceholderFromSttCode() {
        val p = renderBaseSystemPrompt("hi")
        assertFalse(p.contains("{{"), "no placeholders should remain: $p")
        assertContains(p, "Hindi / Hinglish")
    }

    @Test
    fun englishCodeUsesEnglishLabel() {
        val p = renderBaseSystemPrompt("en")
        assertContains(p, "English")
    }

    @Test
    fun autoModeUsesDetectPhrase() {
        val p = renderBaseSystemPrompt("auto")
        assertContains(p, "the language(s) that make up most of the transcript")
    }

    @Test
    fun noScriptRuleWordingRemains() {
        val p = renderBaseSystemPrompt("hi")
        assertFalse(p.contains("Output script"), "script-rule feature removed: $p")
        assertFalse(p.contains("Latin"), "script-rule wording removed: $p")
    }

    @Test
    fun unknownCodeFallsBackToTheCodeItself() {
        assertContains(renderBaseSystemPrompt("zz"), "zz")
    }

    @Test
    fun buildStacksProfileBodyOnTop() {
        val built = buildSystemPrompt("Turn it into a list.", "hi")
        assertTrue(built.trimEnd().endsWith("Turn it into a list."))
        assertContains(built, "You clean up dictated speech-to-text transcripts.")
    }
}
