package com.bolobhai.platform.win

import kotlin.test.Test
import kotlin.test.assertEquals

class WinKeysTest {

    private val altVks = dictationVks(DictationKey.Alt)

    @Test
    fun topRowDigits_classify() {
        assertEquals(RawKey.Digit(1), classifyVk(0x31, altVks))
        assertEquals(RawKey.Digit(5), classifyVk(0x35, altVks))
    }

    @Test
    fun numpadDigits_classify() {
        assertEquals(RawKey.Digit(1), classifyVk(0x61, altVks))
        assertEquals(RawKey.Digit(5), classifyVk(0x65, altVks))
    }

    @Test
    fun esc_classifies() {
        assertEquals(RawKey.Esc, classifyVk(Vk.ESCAPE, altVks))
    }

    @Test
    fun altVks_areDictation() {
        assertEquals(RawKey.Dictation, classifyVk(Vk.LMENU, altVks))
        assertEquals(RawKey.Dictation, classifyVk(Vk.RMENU, altVks))
    }

    @Test
    fun rightCtrlMode_onlyRightCtrlIsDictation() {
        val rc = dictationVks(DictationKey.RightCtrl)
        assertEquals(RawKey.Dictation, classifyVk(Vk.RCONTROL, rc))
        assertEquals(RawKey.Other, classifyVk(Vk.LCONTROL, rc))
    }

    @Test
    fun otherKeys_areOther() {
        assertEquals(RawKey.Other, classifyVk(Vk.TAB, altVks))
        assertEquals(RawKey.Other, classifyVk(0x36, altVks)) // '6'
        assertEquals(RawKey.Other, classifyVk(0x41, altVks)) // 'A'
    }

    @Test
    fun settingParsing() {
        assertEquals(DictationKey.Alt, dictationKeyFromSetting("Alt"))
        assertEquals(DictationKey.RightCtrl, dictationKeyFromSetting("Right Ctrl"))
        assertEquals(DictationKey.Fn, dictationKeyFromSetting("Fn"))
    }
}
