package br.dev.ddr.windowtabs

import com.intellij.util.ui.JBUI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage
import javax.swing.JLabel
import javax.swing.JViewport
import javax.swing.SwingConstants

class TabStripTest {
    private val minimumWidth = JBUI.scale(TabStrip.MINIMUM_TAB_WIDTH)

    private fun strip(tabCount: Int): TabStrip {
        val strip = TabStrip()
        repeat(tabCount) { number: Int -> strip.add(JLabel("tab $number")) }

        return strip
    }

    private fun inViewport(
        strip: TabStrip,
        width: Int,
    ): JViewport {
        val viewport = JViewport()
        viewport.view = strip
        viewport.setSize(width, 30)
        strip.size = Dimension(maxOf(width, strip.preferredSize.width), 30)
        strip.doLayout()

        return viewport
    }

    @Test
    fun `stretches over the viewport while the tabs fit and scrolls once they do not`() {
        val fitting = strip(2)
        inViewport(fitting, minimumWidth * 3)
        assertTrue(fitting.scrollableTracksViewportWidth)

        val crowded = strip(5)
        inViewport(crowded, minimumWidth * 3)
        assertFalse(crowded.scrollableTracksViewportWidth)

        assertTrue(strip(1).scrollableTracksViewportWidth)
        assertTrue(crowded.scrollableTracksViewportHeight)
        assertEquals(crowded.preferredSize, crowded.preferredScrollableViewportSize)
    }

    @Test
    fun `a dragged tab follows the mouse and drops into its slot`() {
        val strip = strip(3)
        inViewport(strip, minimumWidth * 3)
        val first = strip.getComponent(0)

        strip.dragTo(first, minimumWidth + 10)
        strip.doLayout()

        assertSame(first, strip.draggedTab)
        assertEquals(minimumWidth + 10, first.x)
        assertEquals(1, strip.drop())
        assertNull(strip.draggedTab)
        assertSame(first, strip.getComponent(1))
    }

    @Test
    fun `dropping without a drag moves nothing`() {
        val strip = strip(2)

        assertNull(strip.drop())
        assertEquals("tab 0", (strip.getComponent(0) as JLabel).text)
    }

    @Test
    fun `clearing the tabs forgets the drag`() {
        val strip = strip(2)
        strip.dragTo(strip.getComponent(0), 10)

        strip.removeAll()

        assertNull(strip.draggedTab)
        assertEquals(0, strip.componentCount)
    }

    @Test
    fun `paints the dragged tab over its neighbours`() {
        val strip = TabStrip()
        val painted = mutableListOf<String>()
        repeat(2) { number: Int ->
            strip.add(
                object : JLabel("tab $number") {
                    override fun paint(graphics: Graphics) {
                        painted.add(text)
                    }
                },
            )
        }
        inViewport(strip, minimumWidth * 2)
        val image = BufferedImage(minimumWidth * 2, 30, BufferedImage.TYPE_INT_ARGB)

        strip.paint(image.createGraphics())
        assertEquals(listOf("tab 1", "tab 0"), painted)

        painted.clear()
        strip.dragTo(strip.getComponent(1), minimumWidth / 2)
        strip.doLayout()
        strip.paint(image.createGraphics())
        assertEquals("tab 1", painted.last())
    }

    @Test
    fun `the wheel scrolls sideways within the strip`() {
        val strip = strip(6)
        val viewport = inViewport(strip, minimumWidth * 2)
        val step = strip.getScrollableUnitIncrement(Rectangle(), SwingConstants.HORIZONTAL, 1)

        strip.scrollBy(1)
        assertEquals(Point(step, 0), viewport.viewPosition)

        strip.scrollBy(-10)
        assertEquals(Point(0, 0), viewport.viewPosition)

        strip.scrollBy(100)
        assertEquals(Point(minimumWidth * 4, 0), viewport.viewPosition)

        val blockIncrement = strip.getScrollableBlockIncrement(viewport.viewRect, SwingConstants.HORIZONTAL, 1)
        assertEquals(minimumWidth * 2, blockIncrement)
    }

    @Test
    fun `the wheel does nothing outside a viewport`() {
        val strip = strip(2)

        strip.scrollBy(3)

        assertEquals(Point(0, 0), strip.location)
    }

    @Test
    fun `keeps the selected tab in view after a layout`() {
        val strip = strip(6)
        val viewport = inViewport(strip, minimumWidth * 2)

        strip.selectedIndex = 5
        strip.doLayout()

        assertEquals(Point(minimumWidth * 4, 0), viewport.viewPosition)
    }

    @Test
    fun `scrolls only once per new selection`() {
        val strip = strip(6)
        val viewport = inViewport(strip, minimumWidth * 2)
        strip.selectedIndex = 5
        strip.doLayout()

        viewport.viewPosition = Point(0, 0)
        strip.doLayout()

        assertEquals(Point(0, 0), viewport.viewPosition)
    }

    @Test
    fun `a missing or stale selection scrolls nowhere`() {
        val strip = strip(6)
        val viewport = inViewport(strip, minimumWidth * 2)

        strip.selectedIndex = null
        strip.doLayout()
        strip.selectedIndex = 9
        strip.doLayout()

        assertEquals(Point(0, 0), viewport.viewPosition)
    }
}
