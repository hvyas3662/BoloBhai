package com.bolobhai.app

data class QuickSlotsReconcile(val slots: Map<String, String?>, val removed: List<Int>)

/**
 * Clears any slot whose profile file no longer exists (BoloBhai.md §11) and reports which slot
 * numbers were cleared (the caller toasts them).
 */
fun reconcileQuickSlots(quickSlots: Map<String, String?>, available: Set<String>): QuickSlotsReconcile {
    val removed = mutableListOf<Int>()
    val next = LinkedHashMap<String, String?>()
    for (n in 1..5) {
        val key = n.toString()
        val file = quickSlots[key]
        if (file != null && file !in available) {
            next[key] = null
            removed += n
        } else {
            next[key] = file
        }
    }
    return QuickSlotsReconcile(next, removed)
}
