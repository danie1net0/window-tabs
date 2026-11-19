package br.dev.ddr.windowtabs

import com.intellij.ui.DeferredIcon
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
import java.util.concurrent.ConcurrentHashMap
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

        private val colors: MutableMap<String, Color> = ConcurrentHashMap()
        private val ownIcons: MutableMap<String, Boolean> = ConcurrentHashMap()

        /** Whether the project at [key] has its own icon, looked up on disk once rather than on every refresh. */
        public fun hasOwnIcon(
            key: String,
            lookUp: () -> Boolean,
        ): Boolean = ownIcons.getOrPut(key, lookUp)

        /** Stands in when the IDE's icon cannot be read, so one bad project icon does not break the strip. */
        public val FALLBACK_COLOR: Color = Color.GRAY

        /**
         * The colour of the IDE's generated icon for the project at [key], worked out once: computing that icon
         * touches the disk, and every strip asks again on each refresh.
         */
        public fun colorFor(
            key: String,
            ideIcon: () -> Icon,
        ): Color = colors.getOrPut(key) { runCatching { colorOf(ideIcon()) }.getOrDefault(FALLBACK_COLOR) }

        /** The first letter of each of the first two words, as the IDE's own project icons show them. */
        public fun initialsOf(name: String): String =
            name
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { word: String -> word.isNotEmpty() }
                .take(MAX_INITIALS)
                .joinToString("") { word: String -> word.first().uppercase() }

        /**
         * The fill of the IDE's generated icon, the middle of its left-to-right gradient, so the badge keeps
         * that colour.
         * The IDE hands out a deferred icon that paints nothing until computed, so it is computed here first.
         */
        public fun colorOf(icon: Icon): Color {
            val solid: Icon =
                when (icon) {
                    is DeferredIcon -> icon.evaluate()
                    else -> icon
                }
            val image = BufferedImage(solid.iconWidth, solid.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val imageGraphics: Graphics2D = image.createGraphics()
            solid.paintIcon(null, imageGraphics, 0, 0)
            imageGraphics.dispose()

            val left = Color(image.getRGB(SAMPLE_INSET, image.height / 2), true)
            val right = Color(image.getRGB(image.width - 1 - SAMPLE_INSET, image.height / 2), true)

            return Color(
                (left.red + right.red) / 2,
                (left.green + right.green) / 2,
                (left.blue + right.blue) / 2,
                minOf(left.alpha, right.alpha),
            )
        }

        /** One pixel in, past any antialiased rim. */
        private const val SAMPLE_INSET: Int = 1
    }
}
