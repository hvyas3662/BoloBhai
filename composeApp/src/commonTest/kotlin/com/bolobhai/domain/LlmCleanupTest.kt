package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LlmCleanupTest {

    @Test
    fun stripsThinkBlocks() {
        assertEquals("Hello bhai", cleanupLlmOutput("<think>reasoning here</think>\nHello bhai"))
    }

    @Test
    fun stripsMultilineThink() {
        assertEquals("Done", cleanupLlmOutput("<think>\nline1\nline2\n</think>\n\nDone"))
    }

    @Test
    fun stripsHereIsPreamble() {
        assertEquals("Dear team, kal meeting hai.", cleanupLlmOutput("Here is the email:\n\nDear team, kal meeting hai."))
        assertEquals("ok", cleanupLlmOutput("Here's the cleaned text:\nok"))
    }

    @Test
    fun stripsSurroundingQuotes() {
        assertEquals("kal meeting hai", cleanupLlmOutput("\"kal meeting hai\""))
        assertEquals("hi", cleanupLlmOutput("'hi'"))
    }

    @Test
    fun plainText_unchanged() {
        assertEquals("kal meeting hai", cleanupLlmOutput("kal meeting hai"))
    }

    @Test
    fun combined() {
        assertEquals("Final text", cleanupLlmOutput("<think>x</think>\nHere is the text:\n\"Final text\""))
    }
}
