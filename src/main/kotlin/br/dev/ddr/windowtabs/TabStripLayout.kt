package br.dev.ddr.windowtabs

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

/**
 * The preferred width is what every tab needs at [minimumTabWidth], so the scroll pane knows when they overflow.
 * While a tab is dragged it follows the mouse and the others slide aside to open its drop slot.
 */
public class TabStripLayout(
    private val minimumTabWidth: () -> Int,
    private val drag: () -> TabDrag? = { null },
) : LayoutManager {
    override fun addLayoutComponent(
        name: String?,
        component: Component?,
    ): Unit = Unit

    override fun removeLayoutComponent(component: Component?): Unit = Unit

    override fun preferredLayoutSize(parent: Container): Dimension {
        val height: Int =
            parent.components.fold(0) { tallest: Int, component: Component ->
                maxOf(tallest, component.preferredSize.height)
            }

        return Dimension(parent.componentCount * minimumTabWidth(), height)
    }

    override fun minimumLayoutSize(parent: Container): Dimension = preferredLayoutSize(parent)

    override fun layoutContainer(parent: Container) {
        val tabCount: Int = parent.componentCount

        if (tabCount == 0) {
            return
        }

        val tabWidth: Int = tabWidth(parent)
        slots(parent).forEachIndexed { index: Int, component: Component ->
            component.setBounds(index * tabWidth, 0, tabWidth, parent.height)
        }

        val current: TabDrag = drag() ?: return
        current.tab.setBounds(draggedLeft(parent, current), 0, tabWidth, parent.height)
    }

    /** The slot the dragged tab lands in: the one its middle is over. */
    public fun dropIndex(
        parent: Container,
        current: TabDrag,
    ): Int {
        val tabWidth: Int = tabWidth(parent)

        return (draggedLeft(parent, current) + tabWidth / 2) / tabWidth
    }

    private fun slots(parent: Container): List<Component> {
        val components: List<Component> = parent.components.toList()
        val current: TabDrag = drag() ?: return components

        val others: MutableList<Component> =
            components.filterNot { tab: Component -> tab === current.tab }.toMutableList()
        others.add(dropIndex(parent, current), current.tab)

        return others
    }

    private fun draggedLeft(
        parent: Container,
        current: TabDrag,
    ): Int = current.left.coerceIn(0, (parent.componentCount - 1) * tabWidth(parent))

    private fun tabWidth(parent: Container): Int = maxOf(parent.width / parent.componentCount, minimumTabWidth())
}
