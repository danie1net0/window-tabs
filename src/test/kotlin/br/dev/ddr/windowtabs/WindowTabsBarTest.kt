package br.dev.ddr.windowtabs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.awt.Component
import java.awt.Cursor
import java.awt.Frame
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

class WindowTabsBarTest : WindowTabsTestCase() {
    fun `test hidden while only one project is open`() {
        val bar = WindowTabsBar(project)

        assertFalse(bar.isVisible)
        assertEquals(1, bar.tabs().size)
    }

    fun `test one tab per project with the frame's own project selected`() {
        val other = openAnotherProject("other")
        val bar = WindowTabsBar(project)

        assertTrue(bar.isVisible)
        assertEquals(listOf(project.name, "other"), bar.tabs().map { tab -> tab.title().text })
        assertEquals(project.basePath, bar.tabs()[0].toolTipText)
        assertEquals(other.basePath, bar.tabs()[1].toolTipText)
        assertEquals(1, WindowTabsBar(other).strip().selectedIndex)
        assertTrue(
            bar
                .tabs()[0]
                .title()
                .font.isBold,
        )
        assertFalse(
            bar
                .tabs()[1]
                .title()
                .font.isBold,
        )
    }

    fun `test a project still reopening shows as a dimmed tab with a spinner`() {
        WindowTabsService.getInstance().expectReopening(listOf(WindowTab.Reopening("/work/later", "later")))

        val bar = WindowTabsBar(project)
        val placeholder = bar.tabs()[1]

        assertTrue(bar.isVisible)
        assertEquals("later", placeholder.title().text)
        assertEquals("Reopening /work/later", placeholder.toolTipText)
        assertEquals(
            UIUtil.getLabelDisabledForeground(),
            placeholder.title().foreground,
        )
        assertSame(
            AnimatedIcon.Default.INSTANCE,
            placeholder.components
                .filterIsInstance<JBLabel>()
                .first { label -> label.text.isNullOrEmpty() }
                .icon,
        )
    }

    fun `test refreshes when told the tabs changed while shown`() {
        val bar = WindowTabsBar(project)
        bar.addNotify()
        openAnotherProject("other")

        WindowTabsService.getInstance().notifyChanged()
        dispatchEvents()

        assertEquals(2, bar.tabs().size)
    }

    fun `test stops listening once removed`() {
        val bar = WindowTabsBar(project)
        bar.addNotify()
        bar.removeNotify()
        openAnotherProject("other")

        WindowTabsService.getInstance().notifyChanged()
        dispatchEvents()

        assertEquals(1, bar.tabs().size)
        bar.removeNotify()
    }

    fun `test a disposed project builds no tabs`() {
        val disposed = closedProject()

        val bar = WindowTabsBar(disposed)

        assertEmpty(bar.tabs())
    }

    fun `test clicking another tab switches to its project`() {
        val other = openAnotherProject("other")
        val windowManager = fakeWindowManager()
        val source = fakeFrame()
        val destination = fakeFrame()
        `when`(windowManager.getFrame(project)).thenReturn(source)
        `when`(windowManager.getFrame(other)).thenReturn(destination)
        val bar = WindowTabsBar(project)

        click(bar.tabs()[1], MouseEvent.BUTTON1)

        verify(destination, times(2)).extendedState = Frame.NORMAL
        assertEquals(listOf(other), focused)
    }

    fun `test clicking the selected tab or with the right button does nothing`() {
        val other = openAnotherProject("other")
        val windowManager = fakeWindowManager()
        val bar = WindowTabsBar(project)

        click(bar.tabs()[0], MouseEvent.BUTTON1)
        click(bar.tabs()[1], MouseEvent.BUTTON3)
        dispatchEvents()

        verifyNoInteractions(windowManager)
        assertFalse(other.isDisposed)
    }

    fun `test middle click closes the tab's project`() {
        val other = openAnotherProject("other")
        val bar = WindowTabsBar(project)

        click(bar.tabs()[1], MouseEvent.BUTTON2)
        dispatchEvents()

        assertTrue(other.isDisposed)
    }

    fun `test close button shows on hover and closes the project`() {
        val other = openAnotherProject("other")
        val bar = WindowTabsBar(project)
        val tab = bar.tabs()[1]
        tab.setSize(200, 30)
        val close = tab.closeButton()

        mouse(tab, MouseEvent.MOUSE_ENTERED, 10, 10)
        assertTrue(close.isVisible)
        mouse(close, MouseEvent.MOUSE_ENTERED, 1, 1)
        assertSame(AllIcons.Actions.CloseHovered, close.icon)
        mouse(close, MouseEvent.MOUSE_EXITED, 1, 1)
        assertSame(AllIcons.Actions.Close, close.icon)
        mouse(tab, MouseEvent.MOUSE_EXITED, -5, -5)
        assertFalse(close.isVisible)

        click(close, MouseEvent.BUTTON1)
        dispatchEvents()

        assertTrue(other.isDisposed)
    }

    fun `test plus button runs the open project action`() {
        val calls = mutableListOf<AnActionEvent>()
        val recorder =
            object : AnAction() {
                override fun actionPerformed(event: AnActionEvent) {
                    calls.add(event)
                }
            }
        val actionManager = ActionManager.getInstance()
        val original = actionManager.getAction(WindowTabsBar.OPEN_PROJECT_ACTION)
        actionManager.replaceAction(WindowTabsBar.OPEN_PROJECT_ACTION, recorder)
        try {
            click(WindowTabsBar(project).plusButton(), MouseEvent.BUTTON1)
            dispatchEvents()
        } finally {
            actionManager.replaceAction(WindowTabsBar.OPEN_PROJECT_ACTION, original)
        }

        assertEquals(1, calls.size)
    }

    fun `test plus button does nothing when the open project action is missing`() {
        val actionManager = ActionManager.getInstance()
        val original = actionManager.getAction(WindowTabsBar.OPEN_PROJECT_ACTION)
        actionManager.unregisterAction(WindowTabsBar.OPEN_PROJECT_ACTION)
        try {
            click(WindowTabsBar(project).plusButton(), MouseEvent.BUTTON1)
            dispatchEvents()
        } finally {
            actionManager.registerAction(WindowTabsBar.OPEN_PROJECT_ACTION, original)
        }
    }

    fun `test paints selected, hovered, plain and reopening tabs`() {
        openAnotherProject("hovered")
        openAnotherProject("plain")
        WindowTabsService.getInstance().expectReopening(listOf(WindowTab.Reopening("/work/later", "later")))
        val bar = WindowTabsBar(project)
        bar.setSize(600, 30)
        bar.doLayout()
        bar.tabs().forEach { tab ->
            tab.setSize(200, 30)
            tab.doLayout()
        }
        mouse(bar.tabs()[0], MouseEvent.MOUSE_ENTERED, 10, 10)
        mouse(bar.tabs()[1], MouseEvent.MOUSE_ENTERED, 10, 10)

        val image = BufferedImage(600, 30, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        bar.tabs().forEach { tab -> tab.paint(graphics.create()) }
        graphics.dispose()

        assertEquals(
            bar.background,
            JBUI.CurrentTheme.EditorTabs.background(),
        )
    }

    fun `test hidden on macOS unless the settings ask for it`() {
        openAnotherProject("other")
        WindowTabsService.getInstance().isMac = true

        assertFalse(WindowTabsBar(project).isVisible)

        WindowTabsSettings.getInstance().state.showOnMac = true
        assertTrue(WindowTabsBar(project).isVisible)
    }

    fun `test shows a single project when the settings ask for it`() {
        WindowTabsSettings.getInstance().state.showWithSingleProject = true

        assertTrue(WindowTabsBar(project).isVisible)
    }

    fun `test tabs carry the project icon unless turned off`() {
        assertNotNull(WindowTabsBar(project).tabs()[0].title().icon)

        WindowTabsSettings.getInstance().state.showProjectIcons = false
        assertNull(WindowTabsBar(project).tabs()[0].title().icon)
    }

    fun `test dragging a tab drops it where the mouse lets go`() {
        val second = openAnotherProject("second")
        val bar = laidOut(WindowTabsBar(project))
        val first = bar.tabs()[0]

        drag(first, 10, first.width + 10)

        assertEquals(listOf(second, project), openProjects())
        assertEquals(Cursor.getDefaultCursor(), first.cursor)
    }

    fun `test a press that barely moves stays a click`() {
        val second = openAnotherProject("second")
        val bar = laidOut(WindowTabsBar(project))
        val first = bar.tabs()[0]

        drag(first, 10, 12)
        first.dispatchEvent(MouseEvent(first, MouseEvent.MOUSE_DRAGGED, 0, 0, 40, 5, 0, false))

        assertEquals(listOf(project, second), openProjects())
    }

    fun `test the wheel scrolls a crowded strip`() {
        repeat(8) { number -> openAnotherProject("project$number") }
        val bar = laidOut(WindowTabsBar(project))
        val scrollPane = bar.getComponent(0) as JScrollPane

        val unit = MouseWheelEvent.WHEEL_UNIT_SCROLL
        scrollPane.dispatchEvent(
            MouseWheelEvent(scrollPane, MouseWheelEvent.MOUSE_WHEEL, 0, 0, 5, 5, 0, false, unit, 1, 1),
        )
        assertTrue(scrollPane.viewport.viewPosition.x > 0)
    }

    private fun WindowTabsBar.strip(): TabStrip = ((getComponent(0) as JScrollPane).viewport.view as TabStrip)

    private fun WindowTabsBar.tabs(): List<JPanel> = strip().components.map { component -> component as JPanel }

    private fun WindowTabsBar.plusButton(): JBLabel = getComponent(1) as JBLabel

    private fun JPanel.labels(): List<JBLabel> = components.filterIsInstance<JBLabel>()

    private fun JPanel.title(): JBLabel = labels().first { label -> !label.text.isNullOrEmpty() }

    private fun drag(
        tab: JPanel,
        fromX: Int,
        toX: Int,
    ) {
        val down = MouseEvent.BUTTON1_DOWN_MASK
        tab.dispatchEvent(MouseEvent(tab, MouseEvent.MOUSE_PRESSED, 0, down, fromX, 5, 1, false, MouseEvent.BUTTON1))
        tab.dispatchEvent(MouseEvent(tab, MouseEvent.MOUSE_DRAGGED, 0, down, toX, 5, 0, false, MouseEvent.BUTTON1))
        tab.dispatchEvent(MouseEvent(tab, MouseEvent.MOUSE_RELEASED, 0, 0, toX, 5, 1, false, MouseEvent.BUTTON1))
    }

    /** Lays the bar out by hand, as a headless test has no peer to do it; the viewport sizes the strip itself. */
    private fun laidOut(bar: WindowTabsBar): WindowTabsBar {
        val scrollPane = bar.getComponent(0) as JScrollPane
        bar.setSize(600, 30)
        bar.doLayout()
        scrollPane.doLayout()
        scrollPane.viewport.doLayout()
        bar.strip().doLayout()

        return bar
    }

    private fun JPanel.closeButton(): JBLabel = labels().first { label -> label.text.isNullOrEmpty() }

    private fun click(
        target: Component,
        button: Int,
    ) {
        val mask = BUTTON_MASKS.getValue(button)
        target.dispatchEvent(MouseEvent(target, MouseEvent.MOUSE_CLICKED, 0, mask, 1, 1, 1, false, button))
    }

    private fun mouse(
        target: Component,
        id: Int,
        x: Int,
        y: Int,
    ) {
        target.dispatchEvent(MouseEvent(target, id, 0, 0, x, y, 0, false))
        SwingUtilities.invokeLater { }
    }

    private companion object {
        val BUTTON_MASKS =
            mapOf(
                MouseEvent.BUTTON1 to MouseEvent.BUTTON1_DOWN_MASK,
                MouseEvent.BUTTON2 to MouseEvent.BUTTON2_DOWN_MASK,
                MouseEvent.BUTTON3 to MouseEvent.BUTTON3_DOWN_MASK,
            )
    }
}
