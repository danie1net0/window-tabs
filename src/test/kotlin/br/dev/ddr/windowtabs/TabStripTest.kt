package br.dev.ddr.windowtabs

import com.intellij.util.ui.JBUI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import javax.swing.JLabel
import javax.swing.JViewport
import javax.swing.SwingConstants

class TabStripTest {
    private val minWidth = JBUI.scale(TabStrip.MIN_TAB_WIDTH)

    private fun strip(count: Int): TabStrip {
        val strip = TabStrip()
        repeat(count) { strip.add(JLabel("tab $it")) }

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
        inViewport(fitting, minWidth * 3)
        assertTrue(fitting.scrollableTracksViewportWidth)

        val crowded = strip(5)
        inViewport(crowded, minWidth * 3)
        assertFalse(crowded.scrollableTracksViewportWidth)

        assertTrue(strip(1).scrollableTracksViewportWidth)
        assertTrue(crowded.scrollableTracksViewportHeight)
        assertEquals(crowded.preferredSize, crowded.preferredScrollableViewportSize)
    }

    @Test
    fun `finds the tab under a point`() {
        val strip = strip(3)
        inViewport(strip, minWidth * 3)

        assertEquals(0, strip.indexAt(1))
        assertEquals(1, strip.indexAt(minWidth + 1))
        assertEquals(2, strip.indexAt(minWidth * 10))
    }

    @Test
    fun `the wheel scrolls sideways within the strip`() {
        val strip = strip(6)
        val viewport = inViewport(strip, minWidth * 2)
        val step = strip.getScrollableUnitIncrement(Rectangle(), SwingConstants.HORIZONTAL, 1)

        strip.scrollBy(1)
        assertEquals(Point(step, 0), viewport.viewPosition)

        strip.scrollBy(-10)
        assertEquals(Point(0, 0), viewport.viewPosition)

        strip.scrollBy(100)
        assertEquals(Point(minWidth * 4, 0), viewport.viewPosition)

        assertEquals(minWidth * 2, strip.getScrollableBlockIncrement(viewport.viewRect, SwingConstants.HORIZONTAL, 1))
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
        val viewport = inViewport(strip, minWidth * 2)

        strip.selectedIndex = 5
        strip.doLayout()

        assertEquals(Point(minWidth * 4, 0), viewport.viewPosition)
    }

    @Test
    fun `scrolls only once per new selection`() {
        val strip = strip(6)
        val viewport = inViewport(strip, minWidth * 2)
        strip.selectedIndex = 5
        strip.doLayout()

        viewport.viewPosition = Point(0, 0)
        strip.doLayout()

        assertEquals(Point(0, 0), viewport.viewPosition)
    }

    @Test
    fun `a missing or stale selection scrolls nowhere`() {
        val strip = strip(6)
        val viewport = inViewport(strip, minWidth * 2)

        strip.selectedIndex = null
        strip.doLayout()
        strip.selectedIndex = 9
        strip.doLayout()

        assertEquals(Point(0, 0), viewport.viewPosition)
    }
}
