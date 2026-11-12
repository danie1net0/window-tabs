package br.dev.ddr.windowtabs

import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import javax.swing.JPanel
import javax.swing.JViewport
import javax.swing.Scrollable
import javax.swing.SwingConstants

class TabStrip :
    JPanel(TabStripLayout { JBUI.scale(MINIMUM_TAB_WIDTH) }),
    Scrollable {
    var selectedIndex: Int? = null
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

    fun indexAt(horizontalPosition: Int): Int {
        val index: Int = components.indexOfFirst { tab: Component -> horizontalPosition < tab.x + tab.width }

        if (index < 0) {
            return componentCount - 1
        }

        return index
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

    fun scrollBy(wheelRotation: Int) {
        val viewport: JViewport? = parent as? JViewport

        if (viewport == null) {
            return
        }

        val step: Int = getScrollableUnitIncrement(viewport.viewRect, SwingConstants.HORIZONTAL, wheelRotation)
        val farthestLeft: Int = maxOf(0, width - viewport.width)
        val left: Int = (viewport.viewPosition.x + wheelRotation * step).coerceIn(0, farthestLeft)
        viewport.viewPosition = Point(left, 0)
    }

    companion object {
        /** Below this a project name is mostly an ellipsis. */
        const val MINIMUM_TAB_WIDTH = 120
    }
}
