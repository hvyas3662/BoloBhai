package com.bolobhai.platform.mac

import com.bolobhai.domain.HotkeyInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MacHelperTest {

    @Test fun `fn down and up map to key down and up`() {
        assertEquals(HotkeyInput.KeyDown, parseHelperEvent("""{"ev":"fn","down":true,"t":123}"""))
        assertEquals(HotkeyInput.KeyUp, parseHelperEvent("""{"ev":"fn","down":false,"t":123}"""))
    }

    @Test fun `digit keys map to Digit, esc to Esc`() {
        assertEquals(HotkeyInput.Digit(3), parseHelperEvent("""{"ev":"key","k":"3"}"""))
        assertEquals(HotkeyInput.Esc, parseHelperEvent("""{"ev":"key","k":"esc"}"""))
    }

    @Test fun `non-gesture and malformed lines are ignored`() {
        assertNull(parseHelperEvent("""{"ev":"perm","which":"accessibility","granted":false}"""))
        assertNull(parseHelperEvent("""{"ev":"key","k":"9"}"""))
        assertNull(parseHelperEvent("not json"))
    }
}
