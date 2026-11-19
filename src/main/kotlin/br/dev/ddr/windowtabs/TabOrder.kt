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

    /** Closed during this run, oldest first; the saved keys not open now were closed before it and go first. */
    private val closedLately = mutableListOf<String>()

    public fun sync(openKeys: List<String>): List<String> {
        val before: List<String> = keys.toList()
        openKeys.filterNot { key: String -> key in keys }.forEach { key: String -> keys.add(key) }

        val open: Set<String> = openKeys.toSet()
        closedLately.removeAll(open)
        closedLately.addAll(visible.filterNot { key: String -> key in open || key in closedLately })
        forgetSurplus(open)
        visible = keys.filter { key: String -> key in open }

        report(before)

        return visible
    }

    /**
     * Puts [key] in front of the tab [before], or after the last open tab when [before] is null. An anchor key rather
     * than an index, so a tab opening or closing while the user dragged does not shift where the drop lands.
     */
    public fun move(
        key: String,
        before: String?,
    ) {
        if (key !in visible || key == before) {
            return
        }

        val previous: List<String> = keys.toList()
        val others: List<String> = visible.filterNot { other: String -> other == key }
        keys.remove(key)

        val afterLast: Int = others.lastOrNull()?.let { other: String -> keys.indexOf(other) + 1 } ?: 0
        val anchor: String? = before?.takeIf { other: String -> other in keys }
        keys.add(anchor?.let { other: String -> keys.indexOf(other) } ?: afterLast, key)
        val shown: Set<String> = visible.toSet()
        visible = keys.filter { other: String -> other in shown }

        report(previous)
    }

    private fun forgetSurplus(open: Set<String>) {
        val surplus: Int = (keys.size - MAX_REMEMBERED).coerceAtLeast(0)
        val closedBefore: List<String> = keys.filterNot { key: String -> key in open || key in closedLately }
        keys.removeAll((closedBefore + closedLately).take(surplus).toSet())
        closedLately.retainAll(keys.toSet())
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
