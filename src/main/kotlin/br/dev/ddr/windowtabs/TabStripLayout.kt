package br.dev.ddr.windowtabs

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

/** The preferred width is what every tab needs at [minimumTabWidth], so the scroll pane knows when they overflow. */
class TabStripLayout(
    private val minimumTabWidth: () -> Int,
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

        val tabWidth: Int = maxOf(parent.width / tabCount, minimumTabWidth())
        parent.components.forEachIndexed { index: Int, component: Component ->
            component.setBounds(index * tabWidth, 0, tabWidth, parent.height)
        }
    }
}
