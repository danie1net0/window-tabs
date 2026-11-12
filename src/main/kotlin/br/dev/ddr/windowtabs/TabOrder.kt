package br.dev.ddr.windowtabs

class TabOrder(
    initial: List<String> = emptyList(),
    private val changed: (List<String>) -> Unit = {},
) {
    private val keys = initial.distinct().toMutableList()

    fun sync(openKeys: List<String>): List<String> {
        val before = keys.toList()
        keys.retainAll(openKeys.toSet())
        openKeys.filterNot { key -> key in keys }.forEach { key -> keys.add(key) }

        report(before)

        return keys.toList()
    }

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
