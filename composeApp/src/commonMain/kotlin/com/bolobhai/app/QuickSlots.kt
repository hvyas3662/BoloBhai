package com.bolobhai.app

data class QuickSlotsReconcile(val slots: Map<String, String?>, val removed: List<Int>)

/**
 * Assign [fileName] to [slot] (or clear with null), keeping a profile in at most one slot
 * (BoloBhai.md §7): assigning it removes it from any other slot.
 */
fun assignSlotMap(slots: Map<String, String?>, slot: Int, fileName: String?): Map<String, String?> {
    val next = LinkedHashMap(slots)
    if (fileName != null) {
        for (key in next.keys.toList()) if (next[key] == fileName) next[key] = null
    }
    next[slot.toString()] = fileName
    return next
}

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
