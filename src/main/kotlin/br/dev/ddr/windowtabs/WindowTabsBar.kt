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
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.Box
import javax.swing.Icon
import javax.swing.JComponent
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
        border = JBUI.Borders.customLineBottom(EDGE)

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

    override fun getBackground(): Color = recessedBackground()

    /** The line under the toolbar; the selected tab paints over it to merge with the toolbar. */
    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)

        paintTopEdge(graphics)
    }

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

    /** Waits while a tab is dragged: the drop refreshes the strips anyway, and new tabs now would strand the drag. */
    private fun rebuild() {
        if (project.isDisposed || tabs.draggedTab != null) {
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

    private fun view(tab: WindowTab): JPanel = panelFor(tab).apply { putClientProperty(KEY_PROPERTY, tab.key) }

    private fun panelFor(tab: WindowTab): JPanel =
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
            LeftClick { event: MouseEvent ->
                val actionManager: ActionManager = ActionManager.getInstance()
                val openProject: AnAction? = actionManager.getAction(OPEN_PROJECT_ACTION)

                if (openProject == null) {
                    return@LeftClick
                }

                actionManager.tryToExecute(openProject, event, button, ActionPlaces.TOOLBAR, true)
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
        private var grabX: Int? = null
        private val closeButton = JBLabel(AllIcons.Actions.Close)
        private val balance: Component = Box.createRigidArea(closeButton.preferredSize)

        init {
            isOpaque = false
            border = JBUI.Borders.empty(TAB_VERTICAL_PADDING, TAB_HORIZONTAL_PADDING)
            toolTipText = target.basePath

            val title = JBLabel(target.name, projectIcon(), SwingConstants.CENTER)
            title.font = JBUI.Fonts.label().asPlain()
            title.iconTextGap = JBUI.scale(ICON_GAP)
            title.foreground = UIUtil.getContextHelpForeground()

            if (selected) {
                title.foreground = UIUtil.getLabelForeground()
            }

            add(title, BorderLayout.CENTER)

            closeButton.toolTipText = "Close project"
            closeButton.isVisible = false
            balance.isVisible = false

            closeButton.addMouseListener(
                object : MouseAdapter() {
                    override fun mouseEntered(event: MouseEvent) {
                        closeButton.icon = AllIcons.Actions.CloseHovered
                    }

                    override fun mouseExited(event: MouseEvent) {
                        closeButton.icon = AllIcons.Actions.Close
                        updateHover(event)
                    }
                },
            )
            closeButton.addMouseListener(LeftClick { WindowTabsService.getInstance().close(target) })
            add(closeButton, BorderLayout.WEST)
            add(balance, BorderLayout.EAST)

            val mouseHandler =
                object : MouseAdapter() {
                    override fun mouseEntered(event: MouseEvent): Unit = updateHover(event)

                    override fun mouseExited(event: MouseEvent): Unit = updateHover(event)

                    override fun mousePressed(event: MouseEvent): Unit = press(event)

                    override fun mouseDragged(event: MouseEvent): Unit = drag(event)

                    override fun mouseReleased(event: MouseEvent): Unit = release(event)
                }
            addMouseListener(mouseHandler)
            addMouseMotionListener(mouseHandler)
        }

        private fun projectIcon(): Icon? {
            if (!WindowTabsSettings.getInstance().state.showProjectIcons) {
                return null
            }

            return badgeOrOwnIcon()
        }

        /**
         * A project's own icon (.idea/icon.svg) shows as drawn; the deferred icon loads it off the EDT and scales
         * the unscaled size it is given.
         */
        private fun badgeOrOwnIcon(): Icon {
            val manager: RecentProjectsManagerBase = RecentProjectsManagerBase.getInstanceEx()
            val size: Int = JBUI.scale(ICON_SIZE)

            if (ProjectBadge.hasOwnIcon(tab.key) { manager.hasCustomIcon(target) }) {
                return manager.getProjectIcon(tab.key, true, ICON_SIZE, target.name)
            }

            val color: Color = ProjectBadge.colorFor(tab.key) { manager.getProjectIcon(tab.key, true) }

            return ProjectBadge(ProjectBadge.initialsOf(target.name), color, size, JBUI.scale(ICON_DROP))
        }

        /**
         * A press while another drag is still on means its release never came, as when a dialog took the mouse:
         * that drag is called off and the strip catches up on what it skipped.
         */
        private fun press(event: MouseEvent) {
            grabX = null

            if (event.button != MouseEvent.BUTTON1) {
                return
            }

            if (tabs.draggedTab != null) {
                tabs.removeAll()
                rebuild()

                return
            }

            grabX = event.x
        }

        /** A press that moves far enough sideways becomes a drag, and from then on the tab follows the mouse. */
        private fun drag(event: MouseEvent) {
            val grab: Int = grabX ?: return
            val started: Boolean = tabs.draggedTab === this

            if (!started && abs(event.x - grab) < JBUI.scale(DRAG_THRESHOLD)) {
                return
            }

            val point: Point = SwingUtilities.convertPoint(this, event.point, tabs)
            tabs.dragTo(this, point.x - grab)
        }

        /**
         * Ends a drag, or acts as a click. A click is taken on release rather than from AWT's click event,
         * which X11 drops once the mouse moves a few pixels, short of the distance that starts a drag.
         * No click while a drag is on, nor on a tab the strip replaced while the button was down.
         */
        private fun release(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1 && endDrag()) {
                return
            }

            if (tabs.draggedTab != null || parent !== tabs || !contains(event.point)) {
                return
            }

            click(event)
        }

        /** Drops this tab in front of the tab that ends up after it, the one the user can see it land before. */
        private fun endDrag(): Boolean {
            grabX = null
            val index: Int = tabs.drop(this) ?: return false
            val next: String? =
                tabs.components.getOrNull(index + 1)?.let { view: Component ->
                    (view as JComponent).getClientProperty(KEY_PROPERTY) as String
                }

            WindowTabsService.getInstance().move(project, tab.key, next)

            return true
        }

        private fun click(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON2) {
                WindowTabsService.getInstance().close(target)

                return
            }

            if (event.button != MouseEvent.BUTTON1 || selected) {
                return
            }

            WindowTabsService.getInstance().switchTo(project, target)
        }

        /** Hiding a child only invalidates, so the tab lays out again or the title keeps the close button's room. */
        private fun updateHover(event: MouseEvent) {
            val point: Point = SwingUtilities.convertPoint(event.component, event.point, this)
            hovered = contains(point)
            closeButton.isVisible = hovered
            balance.isVisible = hovered

            revalidate()
            repaint()
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)

            graphics.color = background
            graphics.fillRect(0, 0, width, height)

            if (!selected) {
                paintTopEdge(graphics)
            }

            paintSeparator(graphics)
        }

        override fun getBackground(): Color {
            if (selected) {
                return selectedBackground()
            }

            return unselectedBackground(hovered)
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

            val spinner = JBLabel(AnimatedIcon.Default.INSTANCE)
            add(title, BorderLayout.CENTER)
            add(spinner, BorderLayout.WEST)
            add(Box.createRigidArea(spinner.preferredSize), BorderLayout.EAST)
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)

            graphics.color = recessedBackground()
            graphics.fillRect(0, 0, width, height)
            paintTopEdge(graphics)
            paintSeparator(graphics)
        }
    }

    public companion object {
        public const val OPEN_PROJECT_ACTION: String = "OpenFile"

        /** Each tab's key, so a drop can name the tab it lands in front of. */
        private const val KEY_PROPERTY: String = "windowTabs.key"

        private const val PLUS_PADDING: Int = 10
        private const val TAB_VERTICAL_PADDING: Int = 6
        private const val TAB_HORIZONTAL_PADDING: Int = 8
        private const val ICON_SIZE: Int = 16
        private const val ICON_GAP: Int = 6
        private const val ICON_DROP: Int = 1
        private const val DRAG_THRESHOLD: Int = 8

        private const val LIGHT_RECESS_ALPHA: Int = 30
        private const val DARK_RECESS_ALPHA: Int = 80
        private const val LIGHT_EDGE_ALPHA: Int = 48
        private const val DARK_EDGE_ALPHA: Int = 150
        private const val LIGHT_SHADOW_ALPHA: Int = 22
        private const val DARK_SHADOW_ALPHA: Int = 70
        private const val SHADOW_HEIGHT: Int = 4
        private const val HOVER_MIX: Double = 0.5

        /** Shades the toolbar colour for the tabs behind the selected one, like macOS window tabs. */
        private val RECESS_SHADE: JBColor =
            JBColor(Color(0, 0, 0, LIGHT_RECESS_ALPHA), Color(0, 0, 0, DARK_RECESS_ALPHA))

        /** Darkens whatever is under it, so the lines show on the toolbar colour and on the recessed tabs alike. */
        private val EDGE: JBColor = JBColor(Color(0, 0, 0, LIGHT_EDGE_ALPHA), Color(0, 0, 0, DARK_EDGE_ALPHA))

        private val SHADOW: JBColor = JBColor(Color(0, 0, 0, LIGHT_SHADOW_ALPHA), Color(0, 0, 0, DARK_SHADOW_ALPHA))

        /** The selected tab merges with the toolbar above it. */
        private fun selectedBackground(): Color = JBUI.CurrentTheme.CustomFrameDecorations.mainToolbarBackground(true)

        private fun recessedBackground(): Color = ColorUtil.alphaBlending(RECESS_SHADE, selectedBackground())

        private fun unselectedBackground(hovered: Boolean): Color {
            if (hovered) {
                return ColorUtil.mix(recessedBackground(), selectedBackground(), HOVER_MIX)
            }

            return recessedBackground()
        }

        /** The tabs behind the selected one sit under a line and a soft shadow cast by the toolbar. */
        private fun JPanel.paintTopEdge(graphics: Graphics) {
            val shadowHeight: Int = JBUI.scale(SHADOW_HEIGHT)
            val graphics2D: Graphics2D = graphics.create() as Graphics2D
            val fadedShadow: Color = ColorUtil.withAlpha(SHADOW, 0.0)
            graphics2D.paint = GradientPaint(0f, 1f, SHADOW, 0f, (1 + shadowHeight).toFloat(), fadedShadow)
            graphics2D.fillRect(0, 1, width, shadowHeight)
            graphics2D.color = EDGE
            graphics2D.fillRect(0, 0, width, 1)
            graphics2D.dispose()
        }

        private fun JPanel.paintSeparator(graphics: Graphics) {
            graphics.color = EDGE
            graphics.fillRect(width - 1, 0, 1, height)
        }
    }
}
