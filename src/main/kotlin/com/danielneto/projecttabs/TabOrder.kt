package com.danielneto.projecttabs

/**
 * Keeps the tab order stable while projects open and close: known keys keep their position,
 * new keys go to the end, and keys that are no longer open are dropped.
 */
class TabOrder {
    private val keys = mutableListOf<String>()

    fun sync(openKeys: List<String>): List<String> {
        keys.retainAll(openKeys.toSet())
        openKeys.filterNot { it in keys }.forEach { keys.add(it) }

        return keys.toList()
    }
}
