package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JLabel
import javax.swing.JPanel

class TabStripLayoutTest {
    private fun strip(tabCount: Int): JPanel {
        val panel = JPanel(TabStripLayout { 100 })
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
    fun `ignores the names components are added with`() {
        val layout = TabStripLayout { 100 }
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
