package br.dev.ddr.windowtabs

/**
 * The saved order of the tabs. It remembers projects that are closed too: the IDE closes its projects one by one when
 * it quits, and forgetting each one as it closes would leave next start with little of the order the user set.
 */
public class TabOrder(
    initial: List<String> = emptyList(),
    private val changed: (List<String>) -> Unit = {},
) {
    private val keys = initial.distinct().toMutableList()
    private var visible: List<String> = emptyList()

    public fun sync(openKeys: List<String>): List<String> {
        val before: List<String> = keys.toList()
        openKeys.filterNot { key: String -> key in keys }.forEach { key: String -> keys.add(key) }

        val open: Set<String> = openKeys.toSet()
        val surplus: Int = (keys.size - MAX_REMEMBERED).coerceAtLeast(0)
        keys.removeAll(keys.filterNot { key: String -> key in open }.take(surplus).toSet())
        visible = keys.filter { key: String -> key in open }

        report(before)

        return visible
    }

    /** Puts [key] at [index] among the open tabs, in front of the open tab that held that place. */
    public fun move(
        key: String,
        index: Int,
    ) {
        if (key !in visible) {
            return
        }

        val before: List<String> = keys.toList()
        val others: List<String> = visible.filterNot { other: String -> other == key }
        val target: Int = index.coerceIn(0, others.size)
        val anchor: String? = others.getOrNull(target)

        keys.remove(key)
        val afterLast: Int = others.lastOrNull()?.let { other: String -> keys.indexOf(other) + 1 } ?: 0
        keys.add(anchor?.let { other: String -> keys.indexOf(other) } ?: afterLast, key)
        visible = others.take(target) + key + others.drop(target)

        report(before)
    }

    public companion object {
        /** Past this many, the longest-closed projects make room, so the saved order does not grow forever. */
        public const val MAX_REMEMBERED: Int = 100
    }

    private fun report(before: List<String>) {
        if (before == keys) {
            return
        }

        changed(keys.toList())
    }
}
