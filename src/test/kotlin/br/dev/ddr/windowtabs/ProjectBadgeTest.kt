package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.Icon
import kotlin.math.abs

class ProjectBadgeTest {
    private fun painted(initials: String): BufferedImage {
        val image = BufferedImage(SIZE + 4, SIZE + 4, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        ProjectBadge(initials, Color.BLUE, SIZE).paintIcon(null, graphics, 2, 2)
        graphics.dispose()

        return image
    }

    /** Where the white letters are, as the bounds of every pixel that is mostly white. */
    private fun lettersOf(image: BufferedImage): IntArray {
        val white: List<Pair<Int, Int>> =
            (0 until image.width).flatMap { x: Int ->
                (0 until image.height)
                    .filter { y: Int -> Color(image.getRGB(x, y), true).red > 160 }
                    .map { y: Int -> x to y }
            }

        return intArrayOf(
            white.minOf { point: Pair<Int, Int> -> point.first },
            white.maxOf { point: Pair<Int, Int> -> point.first },
            white.minOf { point: Pair<Int, Int> -> point.second },
            white.maxOf { point: Pair<Int, Int> -> point.second },
        )
    }

    @Test
    fun `is a square of the size it is given`() {
        val badge = ProjectBadge("SP", Color.BLUE, SIZE)

        assertEquals(SIZE, badge.iconWidth)
        assertEquals(SIZE, badge.iconHeight)
        assertEquals("SP", badge.initials)
        assertEquals(Color.BLUE, badge.color)
    }

    @Test
    fun `fills a rounded square and leaves the corners clear`() {
        val image = painted("SP")

        assertEquals(Color.BLUE, Color(image.getRGB(3, SIZE / 2 + 2), true))
        assertTrue(Color(image.getRGB(2, 2), true).alpha < 16)
        assertTrue(Color(image.getRGB(SIZE + 1, SIZE + 1), true).alpha < 16)
    }

    @Test
    fun `centres the letters on both axes`() {
        listOf("SP", "G", "Wy").forEach { initials: String ->
            val letters: IntArray = lettersOf(painted(initials))
            val middle: Double = SIZE / 2 + 2 - 0.5

            assertTrue("$initials across", abs((letters[0] + letters[1]) / 2.0 - middle) <= 1.0)
            assertTrue("$initials down", abs((letters[2] + letters[3]) / 2.0 - middle) <= 1.0)
        }
    }

    @Test
    fun `a drop lowers the square inside a taller icon`() {
        val badge = ProjectBadge("SP", Color.BLUE, SIZE, 2)
        val image = BufferedImage(SIZE, SIZE + 4, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        badge.paintIcon(null, graphics, 0, 0)
        graphics.dispose()

        assertEquals(SIZE, badge.iconWidth)
        assertEquals(SIZE + 4, badge.iconHeight)
        assertTrue(Color(image.getRGB(SIZE / 2, 2), true).alpha < 16)
        assertEquals(Color.BLUE, Color(image.getRGB(SIZE / 2, 5), true))
        assertEquals(Color.BLUE, Color(image.getRGB(SIZE / 2, SIZE + 2), true))
    }

    @Test
    fun `takes the first letter of the first two words`() {
        assertEquals("SP", ProjectBadge.initialsOf("system-pulse"))
        assertEquals("G", ProjectBadge.initialsOf("gatinha"))
        assertEquals("WT", ProjectBadge.initialsOf("window tabs_plugin"))
        assertEquals("É2", ProjectBadge.initialsOf("éclair 2026"))
        assertEquals("", ProjectBadge.initialsOf("--"))
    }

    @Test
    fun `reads the colour of the icon it replaces`() {
        val icon =
            object : Icon {
                override fun getIconWidth(): Int = 20

                override fun getIconHeight(): Int = 12

                override fun paintIcon(
                    component: Component?,
                    graphics: Graphics,
                    x: Int,
                    y: Int,
                ) {
                    graphics.color = Color.MAGENTA
                    graphics.fillRect(x, y, iconWidth, iconHeight)
                    graphics.color = Color.WHITE
                    graphics.fillRect(x + 6, y + 3, 8, 6)
                }
            }

        assertEquals(Color.MAGENTA, ProjectBadge.colorOf(icon))
    }

    @Test
    fun `works out a project's colour once and survives an icon that fails`() {
        var reads = 0
        val icon = {
            reads++
            halves(Color(100, 0, 200))
        }

        assertEquals(Color(100, 0, 200), ProjectBadge.colorFor("/work/once", icon))
        assertEquals(Color(100, 0, 200), ProjectBadge.colorFor("/work/once", icon))
        assertEquals(1, reads)
        assertEquals(ProjectBadge.FALLBACK_COLOR, ProjectBadge.colorFor("/work/broken") { error("unreadable icon") })
    }

    @Test
    fun `averages a gradient and stays clear where either edge is`() {
        assertEquals(Color(150, 50, 100), ProjectBadge.colorOf(halves(Color(200, 100, 0))))
        assertEquals(0, ProjectBadge.colorOf(halves(Color(0, 0, 0, 0))).alpha)
    }

    private fun halves(right: Color): Icon =
        object : Icon {
            override fun getIconWidth(): Int = 10

            override fun getIconHeight(): Int = 10

            override fun paintIcon(
                component: Component?,
                graphics: Graphics,
                x: Int,
                y: Int,
            ) {
                graphics.color = Color(100, 0, 200)
                graphics.fillRect(x, y, 5, iconHeight)
                graphics.color = right
                graphics.fillRect(x + 5, y, 5, iconHeight)
            }
        }

    private companion object {
        const val SIZE: Int = 32
    }
}
