package br.dev.ddr.windowtabs

import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import javax.swing.JPanel
import javax.swing.JViewport
import javax.swing.Scrollable
import javax.swing.SwingConstants

public class TabStrip :
    JPanel(),
    Scrollable {
    private var drag: TabDrag? = null
    private val stripLayout = TabStripLayout({ JBUI.scale(MINIMUM_TAB_WIDTH) }, { drag })

    public val draggedTab: Component?
        get() = drag?.tab

    public var selectedIndex: Int? = null
        set(value) {
            field = value
            scrollPending = true
        }

    /**
     * Scrolling moves the viewport, which lays the scroll pane out again and comes back here;
     * scrolling only once per new selection keeps that from looping.
     */
    private var scrollPending = false

    init {
        isOpaque = false
        layout = stripLayout
    }

    override fun doLayout() {
        super.doLayout()

        if (!scrollPending) {
            return
        }

        scrollPending = false
        val index: Int? = selectedIndex

        if (index == null || index >= componentCount) {
            return
        }

        scrollRectToVisible(getComponent(index).bounds)
    }

    /** Draws [tab] with its left edge at [left] while the other tabs slide aside to open its drop slot. */
    public fun dragTo(
        tab: Component,
        left: Int,
    ) {
        drag = TabDrag(tab, left)

        revalidate()
        repaint()
    }

    /** Moves the dragged tab into its slot right away, so it does not jump back before the strips rebuild. */
    public fun drop(): Int? {
        val current: TabDrag = drag ?: return null
        val index: Int = stripLayout.dropIndex(this, current)
        drag = null

        remove(current.tab)
        add(current.tab, index)
        revalidate()
        repaint()

        return index
    }

    override fun removeAll() {
        drag = null

        super.removeAll()
    }

    /** Paints the dragged tab again on top: Swing paints children in reverse order, so a neighbour would cover it. */
    override fun paintChildren(graphics: Graphics) {
        super.paintChildren(graphics)

        val tab: Component = draggedTab ?: return
        val tabGraphics: Graphics = graphics.create(tab.x, tab.y, tab.width, tab.height)
        tab.paint(tabGraphics)
        tabGraphics.dispose()
    }

    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize

    override fun getScrollableUnitIncrement(
        visibleRect: Rectangle,
        orientation: Int,
        direction: Int,
    ): Int = JBUI.scale(MINIMUM_TAB_WIDTH) / 2

    override fun getScrollableBlockIncrement(
        visibleRect: Rectangle,
        orientation: Int,
        direction: Int,
    ): Int = visibleRect.width

    override fun getScrollableTracksViewportWidth(): Boolean {
        val viewport: JViewport? = parent as? JViewport

        if (viewport == null) {
            return true
        }

        return viewport.width >= preferredSize.width
    }

    override fun getScrollableTracksViewportHeight(): Boolean = true

    public fun scrollBy(wheelRotation: Int) {
        val viewport: JViewport? = parent as? JViewport

        if (viewport == null) {
            return
        }

        val step: Int = getScrollableUnitIncrement(viewport.viewRect, SwingConstants.HORIZONTAL, wheelRotation)
        val farthestLeft: Int = maxOf(0, width - viewport.width)
        val left: Int = (viewport.viewPosition.x + wheelRotation * step).coerceIn(0, farthestLeft)
        viewport.viewPosition = Point(left, 0)
    }

    public companion object {
        /** Below this a project name is mostly an ellipsis. */
        public const val MINIMUM_TAB_WIDTH: Int = 120
    }
}
