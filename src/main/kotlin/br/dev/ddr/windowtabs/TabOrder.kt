package br.dev.ddr.windowtabs

/**
 * Keeps the tab order stable while projects open and close: known keys keep their position,
 * new keys go to the end, and keys that are no longer open are dropped.
 * Starts from the order saved last time and reports every change, so the order survives a restart.
 */
class TabOrder(
    initial: List<String> = emptyList(),
    private val changed: (List<String>) -> Unit = {},
) {
    private val keys = initial.distinct().toMutableList()

    fun sync(openKeys: List<String>): List<String> {
        val before = keys.toList()
        keys.retainAll(openKeys.toSet())
        openKeys.filterNot { it in keys }.forEach { keys.add(it) }

        report(before)

        return keys.toList()
    }

    /** Moves [key] to [index] among the current keys; an unknown key or the same place changes nothing. */
    fun move(
        key: String,
        index: Int,
    ) {
        if (key !in keys) {
            return
        }

        val before = keys.toList()
        keys.remove(key)
        keys.add(index.coerceIn(0, keys.size), key)

        report(before)
    }

    private fun report(before: List<String>) {
        if (before == keys) {
            return
        }

        changed(keys.toList())
    }
}
