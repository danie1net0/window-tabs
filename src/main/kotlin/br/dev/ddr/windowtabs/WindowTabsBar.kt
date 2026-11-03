package br.dev.ddr.windowtabs

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/** One strip per frame; the tab of the frame's own project is the selected one. */
class WindowTabsBar(
    private val project: Project,
) : JPanel(BorderLayout()) {
    private val tabs = JPanel(GridLayout(1, 0))
    private var connection: Disposable? = null

    init {
        isOpaque = true
        border = JBUI.Borders.customLineBottom(JBColor.border())
        add(tabs, BorderLayout.CENTER)
        add(newProjectButton(), BorderLayout.EAST)
        rebuild()
    }

    override fun addNotify() {
        super.addNotify()
        val disposable = Disposer.newDisposable("WindowTabsBar")
        ApplicationManager
            .getApplication()
            .messageBus
            .connect(disposable)
            .subscribe(WindowTabsListener.TOPIC, WindowTabsListener { rebuild() })
        connection = disposable
        rebuild()
    }

    override fun removeNotify() {
        connection?.let { Disposer.dispose(it) }
        connection = null
        super.removeNotify()
    }

    override fun getBackground() = JBUI.CurrentTheme.EditorTabs.background()

    private fun rebuild() {
        if (project.isDisposed) {
            return
        }

        val windowTabs = WindowTabsService.getInstance().tabs(project)
        tabs.removeAll()
        windowTabs.forEach { tabs.add(view(it)) }
        isVisible = windowTabs.size > 1
        revalidate()
        repaint()
    }

    private fun view(tab: WindowTab): JPanel =
        when (tab) {
            is WindowTab.Open -> Tab(tab.project, tab.project == project)
            is WindowTab.Reopening -> Placeholder(tab)
        }

    private fun newProjectButton(): JBLabel {
        val button = JBLabel(AllIcons.General.Add)
        button.border = JBUI.Borders.empty(0, PLUS_PADDING)
        button.toolTipText = "Open project"
        button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        button.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val actionManager = ActionManager.getInstance()
                    actionManager.getAction(OPEN_PROJECT_ACTION)?.let {
                        actionManager.tryToExecute(it, e, button, ActionPlaces.TOOLBAR, true)
                    }
                }
            },
        )

        return button
    }

    private inner class Tab(
        private val target: Project,
        private val selected: Boolean,
    ) : JPanel(BorderLayout()) {
        private var hovered = false
        private val closeButton = JBLabel(AllIcons.Actions.Close)

        init {
            isOpaque = false
            border = JBUI.Borders.empty(TAB_VERTICAL_PADDING, TAB_HORIZONTAL_PADDING)
            toolTipText = target.basePath

            val title = JBLabel(target.name, SwingConstants.CENTER)
            title.font = JBUI.Fonts.label().asPlain()
            title.foreground = UIUtil.getContextHelpForeground()
            if (selected) {
                title.font = JBUI.Fonts.label().asBold()
                title.foreground = UIUtil.getLabelForeground()
            }
            add(title, BorderLayout.CENTER)

            closeButton.toolTipText = "Close project"
            closeButton.isVisible = false
            closeButton.addMouseListener(
                object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        closeButton.icon = AllIcons.Actions.CloseHovered
                    }

                    override fun mouseExited(e: MouseEvent) {
                        closeButton.icon = AllIcons.Actions.Close
                        updateHover(e)
                    }

                    override fun mouseClicked(e: MouseEvent) {
                        WindowTabsService.getInstance().close(target)
                    }
                },
            )
            add(closeButton, BorderLayout.WEST)

            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) = updateHover(e)

                    override fun mouseExited(e: MouseEvent) = updateHover(e)

                    override fun mouseClicked(e: MouseEvent) {
                        if (SwingUtilities.isMiddleMouseButton(e)) {
                            WindowTabsService.getInstance().close(target)

                            return
                        }

                        if (SwingUtilities.isLeftMouseButton(e) && !selected) {
                            WindowTabsService.getInstance().switchTo(project, target)
                        }
                    }
                },
            )
        }

        private fun updateHover(e: MouseEvent) {
            val point = SwingUtilities.convertPoint(e.component, e.point, this)
            hovered = contains(point)
            closeButton.isVisible = hovered
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            if (hovered) {
                g.color = JBUI.CurrentTheme.EditorTabs.hoverBackground()
                g.fillRect(0, 0, width, height)
            }

            if (selected) {
                g.color = JBUI.CurrentTheme.EditorTabs.underlinedTabBackground()
                g.fillRect(0, 0, width, height)
                g.color = JBUI.CurrentTheme.EditorTabs.underlineColor()
                val thickness = JBUI.scale(2)
                g.fillRect(0, height - thickness, width, thickness)
            }

            paintSeparator(g)
        }
    }

    /** A project the IDE has not reopened yet: dimmed, with a spinner, and nothing to click until its window is up. */
    private class Placeholder(
        tab: WindowTab.Reopening,
    ) : JPanel(BorderLayout()) {
        init {
            isOpaque = false
            border = JBUI.Borders.empty(TAB_VERTICAL_PADDING, TAB_HORIZONTAL_PADDING)
            toolTipText = "Reopening ${tab.path}"

            val title = JBLabel(tab.name, SwingConstants.CENTER)
            title.font = JBUI.Fonts.label().asPlain()
            title.foreground = UIUtil.getLabelDisabledForeground()
            add(title, BorderLayout.CENTER)
            add(JBLabel(AnimatedIcon.Default.INSTANCE), BorderLayout.WEST)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            paintSeparator(g)
        }
    }

    companion object {
        const val OPEN_PROJECT_ACTION = "OpenFile"

        private fun JPanel.paintSeparator(g: Graphics) {
            g.color = JBColor.border()
            g.fillRect(width - 1, 0, 1, height)
        }

        private const val PLUS_PADDING = 10
        private const val TAB_VERTICAL_PADDING = 5
        private const val TAB_HORIZONTAL_PADDING = 8
    }
}
