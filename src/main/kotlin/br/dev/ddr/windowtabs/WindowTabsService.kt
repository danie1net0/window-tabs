package br.dev.ddr.windowtabs

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Frame
import java.awt.Rectangle
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.JFrame

@Service(Service.Level.APP)
class WindowTabsService {
    private val order =
        TabOrder(WindowTabsSettings.getInstance().state.tabOrder) { keys: List<String> ->
            WindowTabsSettings.getInstance().state.tabOrder = keys.toMutableList()
        }
    private val reopening = ReopeningProjects()

    /**
     * Runs [Runnable] once the IDE has gone quiet; tests swap it out to end the reopen on demand.
     * The platform's own "last projects reopened" event only exists from 2025.x on, so a quiet period stands in for it.
     */
    internal var scheduleReopenEnd: (Runnable) -> Future<*> = { reopenEnd: Runnable ->
        AppExecutorUtil.getAppScheduledExecutorService().schedule(reopenEnd, REOPEN_QUIET_SECONDS, TimeUnit.SECONDS)
    }

    private var pendingReopenEnd: Future<*>? = null

    /** Whether the current OS counts as macOS for the strip; tests flip it to cover both sides. */
    internal var isMac: Boolean = SystemInfo.isMac

    /** Raises a project's window; tests swap it out because the real one needs real frames. */
    internal var focusWindow: (Project) -> Unit = { project: Project -> ProjectUtil.focusProjectWindow(project, true) }

    init {
        reopening.expect(ReopeningProjects.fromLastSession())
    }

    /** [current] counts as open before ProjectManager lists it, so a window never shows itself as a placeholder. */
    fun tabs(current: Project): List<WindowTab> {
        val openProjects: List<Project> = ProjectManager.getInstance().openProjects.toList() + current

        return tabsOf(openProjects.distinct())
    }

    private fun tabsOf(openProjects: List<Project>): List<WindowTab> {
        val byKey = mutableMapOf<String, WindowTab>()
        openProjects
            .map { project: Project -> WindowTab.Open(project) }
            .forEach { tab: WindowTab.Open -> byKey[tab.key] = tab }
        reopening.remaining(byKey.keys).forEach { tab: WindowTab.Reopening -> byKey[tab.key] = tab }

        return order.sync(byKey.keys.toList()).map { key: String -> byKey.getValue(key) }
    }

    fun showsStrip(tabCount: Int): Boolean {
        val options: WindowTabsSettings.Options = WindowTabsSettings.getInstance().state

        return StripVisibility.shows(tabCount, options, isMac)
    }

    fun move(
        key: String,
        index: Int,
    ) {
        // The order only knows the keys it has synced; a drag may come before any strip asked for its tabs.
        tabsOf(ProjectManager.getInstance().openProjects.toList())
        order.move(key, index)
        notifyChanged()
    }

    internal fun expectReopening(projects: List<WindowTab.Reopening>) {
        reopening.expect(projects)
    }

    /** A project window is up: refresh the strips and wait for the next one before calling the reopen over. */
    fun projectOpened() {
        notifyChanged()
        pendingReopenEnd?.cancel(false)
        pendingReopenEnd = scheduleReopenEnd(Runnable { reopenFinished() })
    }

    fun reopenFinished() {
        reopening.finish()
        notifyChanged()
    }

    /** Refreshes the strips even under a modal dialog or progress, which the IDE may show while it reopens projects. */
    fun notifyChanged() {
        val application: Application = ApplicationManager.getApplication()
        application.invokeLater(
            { application.messageBus.syncPublisher(WindowTabsListener.TOPIC).tabsChanged() },
            ModalityState.any(),
        )
    }

    /** Puts the target frame where the current one is and brings it to the front, so switching feels like a tab. */
    fun switchTo(
        current: Project,
        target: Project,
    ) {
        placeOver(current, target)
        focusWindow(target)
    }

    private fun placeOver(
        current: Project,
        target: Project,
    ) {
        val windowManager: WindowManager = WindowManager.getInstance()
        val source: JFrame? = windowManager.getFrame(current)
        val destination: JFrame? = windowManager.getFrame(target)

        if (source == null || destination == null) {
            return
        }

        val placement: FramePlacement = FramePlacement.matching(source.extendedState, source.bounds)
        destination.extendedState = Frame.NORMAL
        placement.bounds?.let { bounds: Rectangle -> destination.bounds = bounds }
        destination.extendedState = placement.extendedState
    }

    fun close(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) {
                return@invokeLater
            }

            ProjectManager.getInstance().closeAndDispose(project)
        }
    }

    companion object {
        /** No new project window for this long means the IDE is done reopening the last session. */
        private const val REOPEN_QUIET_SECONDS = 30L

        fun getInstance(): WindowTabsService = service()
    }
}
