package br.dev.ddr.windowtabs

public class TabOrder(
    initial: List<String> = emptyList(),
    private val changed: (List<String>) -> Unit = {},
) {
    private val keys = initial.distinct().toMutableList()

    public fun sync(openKeys: List<String>): List<String> {
        val before: List<String> = keys.toList()
        keys.retainAll(openKeys.toSet())
        openKeys.filterNot { key: String -> key in keys }.forEach { key: String -> keys.add(key) }

        report(before)

        return keys.toList()
    }

    public fun move(
        key: String,
        index: Int,
    ) {
        if (key !in keys) {
            return
        }

        val before: List<String> = keys.toList()
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
