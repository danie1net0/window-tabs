package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.awt.Dimension
import java.awt.Frame
import java.awt.Rectangle

class FramePlacementTest {
    @Test
    fun `maximized source moves the target to its screen and keeps it maximized`() {
        val placement =
            FramePlacement.matching(Frame.MAXIMIZED_BOTH, Rectangle(1920, 0, 2560, 1440), Dimension(800, 600))

        assertEquals(FramePlacement(Frame.MAXIMIZED_BOTH, Rectangle(2800, 420, 800, 600)), placement)
    }

    @Test
    fun `a target larger than the source's screen is cut down to it`() {
        val placement =
            FramePlacement.matching(Frame.MAXIMIZED_BOTH, Rectangle(0, 0, 1280, 720), Dimension(1600, 900))

        assertEquals(FramePlacement(Frame.MAXIMIZED_BOTH, Rectangle(0, 0, 1280, 720)), placement)
    }

    @Test
    fun `half maximized source is treated as a normal window`() {
        val placement =
            FramePlacement.matching(Frame.MAXIMIZED_HORIZ, Rectangle(0, 10, 1920, 700), Dimension(800, 600))

        assertEquals(FramePlacement(Frame.NORMAL, Rectangle(0, 10, 1920, 700)), placement)
    }

    @Test
    fun `normal source copies its bounds`() {
        val source = Rectangle(100, 50, 1200, 900)
        val placement = FramePlacement.matching(Frame.NORMAL, source, Dimension(800, 600))

        assertEquals(FramePlacement(Frame.NORMAL, Rectangle(100, 50, 1200, 900)), placement)
        assertNotSame(source, placement.bounds)
    }
}
