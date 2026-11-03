package br.dev.ddr.windowtabs

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.components.JBLabel
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Component
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JPanel
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
        assertEquals(listOf(project.name, "other"), bar.tabs().map { it.title().text })
        assertEquals(project.basePath, bar.tabs()[0].toolTipText)
        assertEquals(other.basePath, bar.tabs()[1].toolTipText)
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

        verify(destination, org.mockito.Mockito.times(2)).extendedState = java.awt.Frame.NORMAL
        assertEquals(listOf(other), focused)
    }

    fun `test clicking the selected tab or with the right button does nothing`() {
        val other = openAnotherProject("other")
        val windowManager = fakeWindowManager()
        val bar = WindowTabsBar(project)

        click(bar.tabs()[0], MouseEvent.BUTTON1)
        click(bar.tabs()[1], MouseEvent.BUTTON3)
        dispatchEvents()

        org.mockito.Mockito.verifyNoInteractions(windowManager)
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
        assertSame(com.intellij.icons.AllIcons.Actions.CloseHovered, close.icon)
        mouse(close, MouseEvent.MOUSE_EXITED, 1, 1)
        assertSame(com.intellij.icons.AllIcons.Actions.Close, close.icon)
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
                override fun actionPerformed(e: AnActionEvent) {
                    calls.add(e)
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

    fun `test paints selected, hovered and plain tabs`() {
        openAnotherProject("hovered")
        openAnotherProject("plain")
        val bar = WindowTabsBar(project)
        bar.setSize(600, 30)
        bar.doLayout()
        bar.tabs().forEach {
            it.setSize(200, 30)
            it.doLayout()
        }
        mouse(bar.tabs()[1], MouseEvent.MOUSE_ENTERED, 10, 10)

        val image = BufferedImage(600, 30, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        bar.tabs().forEach { it.paint(graphics.create()) }
        graphics.dispose()

        assertEquals(
            bar.background,
            com.intellij.util.ui.JBUI.CurrentTheme.EditorTabs
                .background(),
        )
    }

    private fun WindowTabsBar.tabs(): List<JPanel> = (getComponent(0) as JPanel).components.map { it as JPanel }

    private fun WindowTabsBar.plusButton(): JBLabel = getComponent(1) as JBLabel

    private fun JPanel.title(): JBLabel = components.filterIsInstance<JBLabel>().first { !it.text.isNullOrEmpty() }

    private fun JPanel.closeButton(): JBLabel = components.filterIsInstance<JBLabel>().first { it.text.isNullOrEmpty() }

    private fun click(
        target: Component,
        button: Int,
    ) {
        val mask =
            when (button) {
                MouseEvent.BUTTON2 -> MouseEvent.BUTTON2_DOWN_MASK
                MouseEvent.BUTTON3 -> MouseEvent.BUTTON3_DOWN_MASK
                else -> MouseEvent.BUTTON1_DOWN_MASK
            }
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
}
