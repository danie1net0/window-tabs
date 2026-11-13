package br.dev.ddr.windowtabs

import com.intellij.icons.AllIcons
import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import kotlin.math.abs

public class WindowTabsBar(
    private val project: Project,
) : JPanel(BorderLayout()) {
    private val tabs = TabStrip()
    private var connection: Disposable? = null

    init {
        isOpaque = true
        border = JBUI.Borders.customLineBottom(JBColor.border())

        add(scrollPane(), BorderLayout.CENTER)
        add(newProjectButton(), BorderLayout.EAST)

        rebuild()
    }

    override fun addNotify() {
        super.addNotify()

        val disposable: Disposable = Disposer.newDisposable("WindowTabsBar")
        ApplicationManager
            .getApplication()
            .messageBus
            .connect(disposable)
            .subscribe(WindowTabsListener.TOPIC, WindowTabsListener { rebuild() })
        connection = disposable

        rebuild()
    }

    override fun removeNotify() {
        connection?.let { disposable: Disposable -> Disposer.dispose(disposable) }
        connection = null

        super.removeNotify()
    }

    override fun getBackground(): Color = JBUI.CurrentTheme.EditorTabs.background()

    private fun scrollPane(): JScrollPane {
        val scrollPane =
            JScrollPane(
                tabs,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
            )
        scrollPane.border = JBUI.Borders.empty()
        scrollPane.isOpaque = false
        scrollPane.viewport.isOpaque = false
        scrollPane.addMouseWheelListener { wheel: MouseWheelEvent -> tabs.scrollBy(wheel.wheelRotation) }

        return scrollPane
    }

    private fun rebuild() {
        if (project.isDisposed) {
            return
        }

        val service: WindowTabsService = WindowTabsService.getInstance()
        val windowTabs: List<WindowTab> = service.tabs(project)

        tabs.removeAll()
        windowTabs.forEach { tab: WindowTab -> tabs.add(view(tab)) }

        val ownKey: String = WindowTab.Open(project).key
        tabs.selectedIndex = windowTabs.indexOfFirst { tab: WindowTab -> tab.key == ownKey }
        isVisible = service.showsStrip(windowTabs.size)

        revalidate()
        repaint()
    }

    private fun view(tab: WindowTab): JPanel =
        when (tab) {
            is WindowTab.Open -> Tab(tab, tab.project == project)
            is WindowTab.Reopening -> Placeholder(tab)
        }

    private fun newProjectButton(): JBLabel {
        val button = JBLabel(AllIcons.General.Add)
        button.border = JBUI.Borders.empty(0, PLUS_PADDING)
        button.toolTipText = "Open project"
        button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        button.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    val actionManager: ActionManager = ActionManager.getInstance()
                    val openProject: AnAction? = actionManager.getAction(OPEN_PROJECT_ACTION)

                    if (openProject == null) {
                        return
                    }

                    actionManager.tryToExecute(openProject, event, button, ActionPlaces.TOOLBAR, true)
                }
            },
        )

        return button
    }

    private inner class Tab(
        private val tab: WindowTab.Open,
        private val selected: Boolean,
    ) : JPanel(BorderLayout()) {
        private val target: Project = tab.project
        private var hovered = false
        private var dragStartX: Int? = null
        private var dragging = false
        private val closeButton = JBLabel(AllIcons.Actions.Close)

        init {
            isOpaque = false
            border = JBUI.Borders.empty(TAB_VERTICAL_PADDING, TAB_HORIZONTAL_PADDING)
            toolTipText = target.basePath

            val title = JBLabel(target.name, projectIcon(), SwingConstants.CENTER)
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
                    override fun mouseEntered(event: MouseEvent) {
                        closeButton.icon = AllIcons.Actions.CloseHovered
                    }

                    override fun mouseExited(event: MouseEvent) {
                        closeButton.icon = AllIcons.Actions.Close
                        updateHover(event)
                    }

                    override fun mouseClicked(event: MouseEvent) {
                        WindowTabsService.getInstance().close(target)
                    }
                },
            )
            add(closeButton, BorderLayout.WEST)

            val mouseHandler =
                object : MouseAdapter() {
                    override fun mouseEntered(event: MouseEvent): Unit = updateHover(event)

                    override fun mouseExited(event: MouseEvent): Unit = updateHover(event)

                    override fun mouseClicked(event: MouseEvent) {
                        if (SwingUtilities.isMiddleMouseButton(event)) {
                            WindowTabsService.getInstance().close(target)

                            return
                        }

                        if (!SwingUtilities.isLeftMouseButton(event) || selected) {
                            return
                        }

                        WindowTabsService.getInstance().switchTo(project, target)
                    }

                    override fun mousePressed(event: MouseEvent) {
                        dragStartX = event.x
                        dragging = false
                    }

                    override fun mouseDragged(event: MouseEvent): Unit = drag(event)

                    override fun mouseReleased(event: MouseEvent): Unit = drop(event)
                }
            addMouseListener(mouseHandler)
            addMouseMotionListener(mouseHandler)
        }

        private fun projectIcon(): Icon? {
            if (!WindowTabsSettings.getInstance().state.showProjectIcons) {
                return null
            }

            val icon: Icon = RecentProjectsManagerBase.getInstanceEx().getProjectIcon(tab.key, true)

            return IconUtil.toSize(icon, JBUI.scale(ICON_SIZE), JBUI.scale(ICON_SIZE))
        }

        /** A press that moves far enough sideways becomes a drag; until then it may still be a click. */
        private fun drag(event: MouseEvent) {
            val start: Int? = dragStartX

            if (start == null || abs(event.x - start) < JBUI.scale(DRAG_THRESHOLD)) {
                return
            }

            dragging = true
            cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
        }

        private fun drop(event: MouseEvent) {
            val wasDragging: Boolean = dragging
            dragStartX = null
            dragging = false
            cursor = Cursor.getDefaultCursor()

            if (!wasDragging) {
                return
            }

            val point: Point = SwingUtilities.convertPoint(this, event.point, tabs)
            WindowTabsService.getInstance().move(tab.key, tabs.indexAt(point.x))
        }

        private fun updateHover(event: MouseEvent) {
            val point: Point = SwingUtilities.convertPoint(event.component, event.point, this)
            hovered = contains(point)
            closeButton.isVisible = hovered

            repaint()
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)

            if (hovered) {
                graphics.color = JBUI.CurrentTheme.EditorTabs.hoverBackground()
                graphics.fillRect(0, 0, width, height)
            }

            if (selected) {
                graphics.color = JBUI.CurrentTheme.EditorTabs.underlinedTabBackground()
                graphics.fillRect(0, 0, width, height)

                graphics.color = JBUI.CurrentTheme.EditorTabs.underlineColor()
                val thickness: Int = JBUI.scale(2)
                graphics.fillRect(0, height - thickness, width, thickness)
            }

            paintSeparator(graphics)
        }
    }

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

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)

            paintSeparator(graphics)
        }
    }

    public companion object {
        public const val OPEN_PROJECT_ACTION: String = "OpenFile"

        private const val PLUS_PADDING: Int = 10
        private const val TAB_VERTICAL_PADDING: Int = 5
        private const val TAB_HORIZONTAL_PADDING: Int = 8
        private const val ICON_SIZE: Int = 16
        private const val DRAG_THRESHOLD: Int = 8

        private fun JPanel.paintSeparator(graphics: Graphics) {
            graphics.color = JBColor.border()
            graphics.fillRect(width - 1, 0, 1, height)
        }
    }
}
