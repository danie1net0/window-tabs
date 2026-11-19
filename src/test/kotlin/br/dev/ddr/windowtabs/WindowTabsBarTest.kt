package br.dev.ddr.windowtabs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.awt.Color
import java.awt.Component
import java.awt.Frame
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

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
        assertEquals(listOf(project.name, "other"), bar.tabs().map { tab: JPanel -> tab.title().text })
        assertEquals(project.basePath, bar.tabs()[0].toolTipText)
        assertEquals(other.basePath, bar.tabs()[1].toolTipText)
        assertEquals(1, WindowTabsBar(other).strip().selectedIndex)
        assertEquals(UIUtil.getLabelForeground(), bar.tabs()[0].title().foreground)
        assertEquals(UIUtil.getContextHelpForeground(), bar.tabs()[1].title().foreground)
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
                .first { label: JBLabel -> label.text.isNullOrEmpty() }
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
        val bar = laidOut(WindowTabsBar(project))

        release(bar.tabs()[1], MouseEvent.BUTTON1)

        verify(destination, times(2)).extendedState = Frame.NORMAL
        assertEquals(listOf(other), focused)
    }

    fun `test clicking the selected tab or with the right button does nothing`() {
        val other = openAnotherProject("other")
        val windowManager = fakeWindowManager()
        val bar = laidOut(WindowTabsBar(project))

        release(bar.tabs()[0], MouseEvent.BUTTON1)
        release(bar.tabs()[1], MouseEvent.BUTTON3)
        release(bar.tabs()[1], MouseEvent.BUTTON1, -5)
        release(bar.tabs()[1], MouseEvent.BUTTON2, -5)
        dispatchEvents()

        verifyNoInteractions(windowManager)
        assertFalse(other.isDisposed)
    }

    fun `test middle click closes the tab's project`() {
        val other = openAnotherProject("other")
        val bar = laidOut(WindowTabsBar(project))

        release(bar.tabs()[1], MouseEvent.BUTTON2)
        dispatchEvents()

        assertTrue(other.isDisposed)
    }

    fun `test right-clicking the close button keeps the project open`() {
        val other = openAnotherProject("other")
        val bar = WindowTabsBar(project)

        click(bar.tabs()[1].closeButton(), MouseEvent.BUTTON3)
        dispatchEvents()

        assertFalse(other.isDisposed)
    }

    fun `test a press after a drag whose release never came calls the drag off`() {
        val second = openAnotherProject("second")
        val bar = laidOut(WindowTabsBar(project))
        val first = bar.tabs()[0]
        press(first, 10)
        move(first, first.width / 2)

        press(bar.tabs()[1], 10)

        assertNull(bar.strip().draggedTab)
        assertNotSame(first, bar.tabs()[0])
        assertEquals(listOf(project, second), openProjects())
    }

    fun `test letting go over a tab the strip has since replaced does nothing`() {
        val other = openAnotherProject("other")
        val windowManager = fakeWindowManager()
        val bar = laidOut(WindowTabsBar(project))
        val stale = bar.tabs()[1]
        press(bar.tabs()[0], 10)
        move(bar.tabs()[0], bar.tabs()[0].width / 2)
        press(stale, 10)

        release(stale, MouseEvent.BUTTON1)
        dispatchEvents()

        verifyNoInteractions(windowManager)
        assertFalse(other.isDisposed)
    }

    fun `test dragging with another button reorders nothing`() {
        val second = openAnotherProject("second")
        val bar = laidOut(WindowTabsBar(project))
        val first = bar.tabs()[0]
        val pressed: Int = MouseEvent.BUTTON3_DOWN_MASK
        val button: Int = MouseEvent.BUTTON3

        first.dispatchEvent(MouseEvent(first, MouseEvent.MOUSE_PRESSED, 0, pressed, 10, 5, 1, false, button))
        val far: Int = first.width + 10
        first.dispatchEvent(MouseEvent(first, MouseEvent.MOUSE_DRAGGED, 0, pressed, far, 5, 0, false, button))

        assertNull(bar.strip().draggedTab)
        assertEquals(listOf(project, second), openProjects())
    }

    fun `test the strip waits for the drop before it refreshes`() {
        val second = openAnotherProject("second")
        val bar = laidOut(WindowTabsBar(project))
        val first = bar.tabs()[0]
        press(first, 10)
        move(first, first.width / 2)
        val button: Int = MouseEvent.BUTTON1
        val release = MouseEvent(first, MouseEvent.MOUSE_RELEASED, 0, 0, first.width / 2, 5, 1, false, button)
        bar.addNotify()

        WindowTabsService.getInstance().notifyChanged()
        dispatchEvents()
        assertSame(first, bar.tabs()[0])

        first.mouseListeners
            .filterNot { listener: MouseListener -> listener is ToolTipManager }
            .forEach { listener: MouseListener -> listener.mouseReleased(release) }
        dispatchEvents()

        assertNotSame(first, bar.tabs()[0])
        assertEquals(listOf(project, second), openProjects())
        bar.removeNotify()
    }

    fun `test close button shows on hover and closes the project`() {
        val other = openAnotherProject("other")
        val bar = WindowTabsBar(project)
        val tab = bar.tabs()[1]
        tab.setSize(200, 30)
        val close = tab.closeButton()

        val balance = tab.getComponent(2)

        mouse(tab, MouseEvent.MOUSE_ENTERED, 10, 10)
        assertTrue(close.isVisible)
        assertTrue(balance.isVisible)
        assertEquals(close.preferredSize, balance.preferredSize)
        mouse(close, MouseEvent.MOUSE_ENTERED, 1, 1)
        assertSame(AllIcons.Actions.CloseHovered, close.icon)
        mouse(close, MouseEvent.MOUSE_EXITED, 1, 1)
        assertSame(AllIcons.Actions.Close, close.icon)
        mouse(tab, MouseEvent.MOUSE_EXITED, -5, -5)
        assertFalse(close.isVisible)
        assertFalse(balance.isVisible)

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
        bar.tabs().forEach { tab: JPanel ->
            tab.setSize(200, 30)
            tab.doLayout()
        }
        mouse(bar.tabs()[0], MouseEvent.MOUSE_ENTERED, 10, 10)
        mouse(bar.tabs()[1], MouseEvent.MOUSE_ENTERED, 10, 10)

        val image = BufferedImage(600, 30, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        bar.paint(graphics.create())
        bar.tabs().forEach { tab: JPanel -> tab.paint(graphics.create()) }
        graphics.dispose()

        val toolbar = JBUI.CurrentTheme.CustomFrameDecorations.mainToolbarBackground(true)
        val selected = painted(bar.tabs()[0])
        val plain = painted(bar.tabs()[2])
        assertEquals(toolbar, bar.tabs()[0].background)
        assertFalse(bar.background == toolbar)
        assertEquals(bar.background, bar.tabs()[2].background)
        assertFalse(bar.tabs()[1].background in listOf(toolbar, bar.background))
        assertEquals(Color(selected.getRGB(4, 15), true), toolbar)
        assertEquals(Color(selected.getRGB(100, 0), true), toolbar)
        val plainFill = Color(plain.getRGB(4, 15), true)
        assertEquals(plainFill, bar.background)
        assertTrue(Color(plain.getRGB(100, 0), true).green < plainFill.green)
        assertTrue(Color(plain.getRGB(100, 2), true).green < plainFill.green)
        assertTrue(Color(plain.getRGB(199, 15), true).green < plainFill.green)
    }

    private fun painted(tab: JPanel): BufferedImage {
        val image = BufferedImage(tab.width, tab.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        tab.paint(graphics)
        graphics.dispose()

        return image
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
        assertTrue(WindowTabsBar(project).tabs()[0].title().icon is ProjectBadge)

        WindowTabsSettings.getInstance().state.showProjectIcons = false
        assertNull(WindowTabsBar(project).tabs()[0].title().icon)
    }

    fun `test a project's own icon shows as drawn instead of a badge`() {
        val directory = Files.createTempDirectory("custom")
        Files.createDirectories(directory.resolve(".idea"))
        Files.writeString(
            directory.resolve(".idea/icon.svg"),
            """<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><circle cx="8" cy="8" r="4"/></svg>""",
        )
        val custom = PlatformTestUtil.loadAndOpenProject(directory, testRootDisposable)

        val icon =
            WindowTabsBar(custom)
                .tabs()
                .last()
                .title()
                .icon

        assertNotNull(icon)
        assertFalse(icon is ProjectBadge)
        assertEquals(JBUI.scale(16), icon.iconWidth)
    }

    fun `test dragging a tab drops it where the mouse lets go`() {
        val second = openAnotherProject("second")
        val bar = laidOut(WindowTabsBar(project))
        val first = bar.tabs()[0]

        press(first, 10)
        move(first, first.width / 2)
        bar.strip().doLayout()
        assertEquals(first.width / 2 - 10, first.x)
        assertSame(first, bar.strip().draggedTab)

        move(first, 12)
        bar.strip().doLayout()
        assertEquals(first.width / 2 - 8, first.x)

        move(first, first.width + 10)
        release(first, MouseEvent.BUTTON1, first.width + 10)

        assertEquals(listOf(second, project), openProjects())
        assertSame(first, bar.strip().getComponent(1))
        assertNull(bar.strip().draggedTab)
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
        repeat(8) { number: Int -> openAnotherProject("project$number") }
        val bar = laidOut(WindowTabsBar(project))
        val scrollPane = bar.getComponent(0) as JScrollPane

        val unit = MouseWheelEvent.WHEEL_UNIT_SCROLL
        scrollPane.dispatchEvent(
            MouseWheelEvent(scrollPane, MouseWheelEvent.MOUSE_WHEEL, 0, 0, 5, 5, 0, false, unit, 1, 1),
        )
        assertTrue(scrollPane.viewport.viewPosition.x > 0)
    }

    private fun WindowTabsBar.strip(): TabStrip = ((getComponent(0) as JScrollPane).viewport.view as TabStrip)

    private fun WindowTabsBar.tabs(): List<JPanel> {
        val components: Array<Component> = strip().components

        return components.map { component: Component -> component as JPanel }
    }

    private fun WindowTabsBar.plusButton(): JBLabel = getComponent(1) as JBLabel

    private fun JPanel.labels(): List<JBLabel> = components.filterIsInstance<JBLabel>()

    private fun JPanel.title(): JBLabel = labels().first { label: JBLabel -> !label.text.isNullOrEmpty() }

    private fun drag(
        tab: JPanel,
        startPosition: Int,
        endPosition: Int,
    ) {
        press(tab, startPosition)
        move(tab, endPosition)
        val button: Int = MouseEvent.BUTTON1
        tab.dispatchEvent(MouseEvent(tab, MouseEvent.MOUSE_RELEASED, 0, 0, endPosition, 5, 1, false, button))
    }

    private fun press(
        tab: JPanel,
        position: Int,
    ) {
        val pressed: Int = MouseEvent.BUTTON1_DOWN_MASK
        val button: Int = MouseEvent.BUTTON1
        tab.dispatchEvent(MouseEvent(tab, MouseEvent.MOUSE_PRESSED, 0, pressed, position, 5, 1, false, button))
    }

    /** [position] is relative to where the tab is now, as the mouse moves it along. */
    private fun move(
        tab: JPanel,
        position: Int,
    ) {
        val pressed: Int = MouseEvent.BUTTON1_DOWN_MASK
        val button: Int = MouseEvent.BUTTON1
        tab.dispatchEvent(MouseEvent(tab, MouseEvent.MOUSE_DRAGGED, 0, pressed, position, 5, 0, false, button))
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

    private fun JPanel.closeButton(): JBLabel = labels().first { label: JBLabel -> label.text.isNullOrEmpty() }

    /** Ends a press on [tab], as a click once the mouse lets go over it. */
    private fun release(
        tab: JPanel,
        button: Int,
        horizontalPosition: Int = 1,
    ) {
        val event = MouseEvent(tab, MouseEvent.MOUSE_RELEASED, 0, 0, horizontalPosition, 1, 1, false, button)
        tab.dispatchEvent(event)
    }

    /** Presses and lets go over [target], which gets a size first: a headless test lays nothing out. */
    private fun click(
        target: Component,
        button: Int,
    ) {
        target.setSize(20, 20)
        target.dispatchEvent(MouseEvent(target, MouseEvent.MOUSE_RELEASED, 0, 0, 1, 1, 1, false, button))
    }

    private fun mouse(
        target: Component,
        eventId: Int,
        horizontalPosition: Int,
        verticalPosition: Int,
    ) {
        target.dispatchEvent(MouseEvent(target, eventId, 0, 0, horizontalPosition, verticalPosition, 0, false))
        SwingUtilities.invokeLater { }
    }
}
