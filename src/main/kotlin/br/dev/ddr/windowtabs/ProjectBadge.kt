package br.dev.ddr.windowtabs

import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.GlyphVector
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.swing.Icon

/**
 * A rounded square in [color] with the project's [initials] centred on the letters' own outline, so they sit in the
 * middle whatever the font's ascent and descent. The IDE's generated project icon is not square on every platform and
 * crowds its letters against the edges, so the tabs draw their own.
 *
 * [drop] lowers the square inside a taller icon: a tab's visible area starts under the line and shadow at its top,
 * so a square centred on the tab's full height looks a little high.
 */
public class ProjectBadge(
    public val initials: String,
    public val color: Color,
    private val size: Int,
    private val drop: Int = 0,
) : Icon {
    override fun getIconWidth(): Int = size

    override fun getIconHeight(): Int = size + 2 * drop

    override fun paintIcon(
        component: Component?,
        graphics: Graphics,
        x: Int,
        y: Int,
    ) {
        val badge: Graphics2D = graphics.create() as Graphics2D
        badge.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        badge.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        badge.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        badge.translate(x, y + 2 * drop)

        val side: Float = size.toFloat()
        val arc: Float = side * ARC_RATIO
        badge.color = color
        badge.fill(RoundRectangle2D.Float(0f, 0f, side, side, arc, arc))

        val font: Font = JBUI.Fonts.label().deriveFont(Font.BOLD, side * FONT_RATIO)
        val glyphs: GlyphVector = font.createGlyphVector(badge.fontRenderContext, initials)
        val outline: Rectangle2D = glyphs.visualBounds
        val left: Float = (side / 2 - outline.centerX).toFloat()
        val baseline: Float = (side / 2 - outline.centerY).toFloat()
        badge.color = Color.WHITE
        badge.drawGlyphVector(glyphs, left, baseline)
        badge.dispose()
    }

    public companion object {
        private const val ARC_RATIO: Float = 0.45f
        private const val FONT_RATIO: Float = 0.5f
        private const val MAX_INITIALS: Int = 2

        /** The first letter of each of the first two words, as the IDE's own project icons show them. */
        public fun initialsOf(name: String): String =
            name
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { word: String -> word.isNotEmpty() }
                .take(MAX_INITIALS)
                .joinToString("") { word: String -> word.first().uppercase() }

        /** The fill of the IDE's generated icon, read halfway down its left edge, so the badge keeps that colour. */
        public fun colorOf(icon: Icon): Color {
            val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val imageGraphics: Graphics2D = image.createGraphics()
            icon.paintIcon(null, imageGraphics, 0, 0)
            imageGraphics.dispose()

            return Color(image.getRGB(SAMPLE_INSET, image.height / 2), true)
        }

        /** One pixel in, past any antialiased rim. */
        private const val SAMPLE_INSET: Int = 1
    }
}
