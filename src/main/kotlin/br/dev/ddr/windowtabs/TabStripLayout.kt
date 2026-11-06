package br.dev.ddr.windowtabs

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

/**
 * Lays the tabs side by side with equal widths, never narrower than [minimumTabWidth].
 * The strip's preferred width is what all tabs need at that minimum, so a scroll pane knows when they overflow.
 */
class TabStripLayout(
    private val minimumTabWidth: () -> Int,
) : LayoutManager {
    override fun addLayoutComponent(
        name: String?,
        component: Component?,
    ) = Unit

    override fun removeLayoutComponent(component: Component?) = Unit

    override fun preferredLayoutSize(parent: Container): Dimension {
        val height = parent.components.fold(0) { tallest, component -> maxOf(tallest, component.preferredSize.height) }

        return Dimension(parent.componentCount * minimumTabWidth(), height)
    }

    override fun minimumLayoutSize(parent: Container): Dimension = preferredLayoutSize(parent)

    override fun layoutContainer(parent: Container) {
        val count = parent.componentCount

        if (count == 0) {
            return
        }

        val width = maxOf(parent.width / count, minimumTabWidth())
        parent.components.forEachIndexed { index, component ->
            component.setBounds(index * width, 0, width, parent.height)
        }
    }
}
