package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.awt.Frame
import java.awt.Rectangle

class FramePlacementTest {
    @Test
    fun `maximized source keeps the target maximized without bounds`() {
        val placement = FramePlacement.matching(Frame.MAXIMIZED_BOTH, Rectangle(0, 0, 800, 600))

        assertEquals(FramePlacement(Frame.MAXIMIZED_BOTH, null), placement)
    }

    @Test
    fun `half maximized source is treated as a normal window`() {
        val placement = FramePlacement.matching(Frame.MAXIMIZED_HORIZ, Rectangle(0, 10, 1920, 700))

        assertEquals(FramePlacement(Frame.NORMAL, Rectangle(0, 10, 1920, 700)), placement)
    }

    @Test
    fun `normal source copies its bounds`() {
        val source = Rectangle(100, 50, 1200, 900)
        val placement = FramePlacement.matching(Frame.NORMAL, source)

        assertEquals(FramePlacement(Frame.NORMAL, Rectangle(100, 50, 1200, 900)), placement)
        assertNotSame(source, placement.bounds)
    }
}
