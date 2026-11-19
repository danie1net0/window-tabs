package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JLabel
import javax.swing.JPanel

class TabStripLayoutTest {
    private var drag: TabDrag? = null
    private val layout = TabStripLayout({ 100 }, { drag })

    private fun strip(tabCount: Int): JPanel {
        val panel = JPanel(layout)
        repeat(tabCount) { number: Int ->
            val label = JLabel("tab")
            label.preferredSize = Dimension(50, HEIGHTS[number % HEIGHTS.size])
            panel.add(label)
        }

        return panel
    }

    @Test
    fun `wants the minimum width of every tab and the tallest tab's height`() {
        assertEquals(Dimension(300, 24), strip(3).preferredSize)
        assertEquals(Dimension(300, 24), strip(3).minimumSize)
        assertEquals(Dimension(0, 0), strip(0).preferredSize)
    }

    @Test
    fun `splits a wide strip evenly`() {
        val panel = strip(2)
        panel.setSize(500, 30)

        panel.doLayout()

        assertEquals(Rectangle(0, 0, 250, 30), panel.getComponent(0).bounds)
        assertEquals(Rectangle(250, 0, 250, 30), panel.getComponent(1).bounds)
    }

    @Test
    fun `keeps the minimum width when the tabs do not fit`() {
        val panel = strip(3)
        panel.setSize(200, 30)

        panel.doLayout()

        assertEquals(Rectangle(200, 0, 100, 30), panel.getComponent(2).bounds)
    }

    @Test
    fun `an empty strip lays out nothing`() {
        val panel = strip(0)
        panel.setSize(200, 30)

        panel.doLayout()

        assertEquals(0, panel.componentCount)
    }

    @Test
    fun `the dragged tab follows the mouse while the others open its drop slot`() {
        val panel = strip(3)
        panel.setSize(300, 30)
        drag = TabDrag(panel.getComponent(0), 110)

        panel.doLayout()

        assertEquals(Rectangle(110, 0, 100, 30), panel.getComponent(0).bounds)
        assertEquals(Rectangle(0, 0, 100, 30), panel.getComponent(1).bounds)
        assertEquals(Rectangle(200, 0, 100, 30), panel.getComponent(2).bounds)
        assertEquals(1, layout.dropIndex(panel, TabDrag(panel.getComponent(0), 110)))
    }

    @Test
    fun `the dragged tab stays within the strip`() {
        val panel = strip(3)
        panel.setSize(300, 30)

        drag = TabDrag(panel.getComponent(1), -80)
        panel.doLayout()
        assertEquals(Rectangle(0, 0, 100, 30), panel.getComponent(1).bounds)
        assertEquals(0, layout.dropIndex(panel, TabDrag(panel.getComponent(1), -80)))

        drag = TabDrag(panel.getComponent(1), 900)
        panel.doLayout()
        assertEquals(Rectangle(200, 0, 100, 30), panel.getComponent(1).bounds)
        assertEquals(2, layout.dropIndex(panel, TabDrag(panel.getComponent(1), 900)))
    }

    @Test
    fun `needs no drag source by default`() {
        val panel = JPanel(TabStripLayout({ 100 }))
        panel.add(JLabel("tab"))
        panel.setSize(300, 30)

        panel.doLayout()

        assertEquals(Rectangle(0, 0, 300, 30), panel.getComponent(0).bounds)
    }

    @Test
    fun `ignores the names components are added with`() {
        val label = JLabel()

        layout.addLayoutComponent("name", label)
        layout.removeLayoutComponent(label)

        assertEquals(Dimension(0, 0), layout.preferredLayoutSize(JPanel()))
    }

    private companion object {
        /** Not sorted, so the tallest tab is neither the first nor the last. */
        val HEIGHTS: List<Int> = listOf(22, 24, 20)
    }
}
