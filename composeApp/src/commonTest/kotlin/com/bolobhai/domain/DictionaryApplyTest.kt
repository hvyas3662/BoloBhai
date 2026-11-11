package com.bolobhai.domain

import com.bolobhai.data.Correction
import com.bolobhai.data.Dictionary
import com.bolobhai.data.DictionaryYaml
import kotlin.test.Test
import kotlin.test.assertEquals

class DictionaryApplyTest {

    @Test
    fun `correction is whole-word and case-insensitive`() {
        val dict = Dictionary(corrections = listOf(Correction("trilogic", "Triologic")))
        assertEquals("I joined Triologic", applyCorrections("I joined trilogic", dict))
        assertEquals("I joined Triologic", applyCorrections("I joined Trilogic", dict))
        // substring inside another word is left alone
        assertEquals("trilogical", applyCorrections("trilogical", dict))
    }

    @Test
    fun `multiple corrections apply in order`() {
        val dict = Dictionary(corrections = listOf(
            Correction("almary", "Almirah"),
            Correction("thales", "Tiles"),
        ))
        assertEquals("Almirah and Tiles", applyCorrections("almary and thales", dict))
    }

    @Test
    fun `replacement text is literal, not a regex`() {
        val dict = Dictionary(corrections = listOf(Correction("cost", "\$5")))
        assertEquals("the \$5 is", applyCorrections("the cost is", dict))
    }

    @Test
    fun `blank wrong is skipped`() {
        assertEquals("hi", applyCorrections("hi", Dictionary(corrections = listOf(Correction("", "x")))))
    }

    @Test
    fun `whisperPrompt joins custom words`() {
        assertEquals("Triologic, Kotlin", whisperPrompt(Dictionary(customWords = listOf("Triologic", "Kotlin"))))
    }

    @Test
    fun `yaml round-trips`() {
        val dict = Dictionary(
            customWords = listOf("Triologic", "Bolo Bhai"),
            corrections = listOf(Correction("trilogic", "Triologic")),
        )
        assertEquals(dict, DictionaryYaml.decode(DictionaryYaml.encode(dict)))
    }
}
