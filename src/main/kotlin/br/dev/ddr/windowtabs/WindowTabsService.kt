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
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Frame
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.JFrame

@Service(Service.Level.APP)
public class WindowTabsService {
    /** The service outlives each test while the settings do not, so tests load the order again. */
    internal var order: TabOrder = savedOrder()
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
    public fun tabs(current: Project): List<WindowTab> {
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

    public fun showsStrip(tabCount: Int): Boolean {
        val options: WindowTabsSettings.Options = WindowTabsSettings.getInstance().state

        return StripVisibility.shows(tabCount, options, isMac)
    }

    /**
     * [current] is the window the drag happened in, which may be open before ProjectManager lists it. The tabs are
     * synced first: the order only knows the keys it has synced, and a drag may come before any strip asked for them.
     */
    public fun move(
        current: Project,
        key: String,
        before: String?,
    ) {
        tabs(current)
        order.move(key, before)
        notifyChanged()
    }

    internal fun expectReopening(projects: List<WindowTab.Reopening>) {
        reopening.expect(projects)
    }

    /**
     * A project window is up: refresh the strips and wait for the next one before calling the reopen over.
     * Startup activities of several projects run on background threads at once, so the timer is handled on the EDT.
     */
    public fun projectOpened(project: Project) {
        ApplicationManager.getApplication().invokeLater(
            {
                reopening.opened(WindowTab.Open(project).key)
                notifyChanged()
                pendingReopenEnd?.cancel(false)
                pendingReopenEnd = scheduleReopenEnd(Runnable { reopenFinished() })
            },
            ModalityState.any(),
        )
    }

    /** Runs on the EDT, where the strips read the placeholders, even when the timer fires on a pool thread. */
    public fun reopenFinished() {
        ApplicationManager.getApplication().invokeLater(
            {
                reopening.finish()
                notifyChanged()
            },
            ModalityState.any(),
        )
    }

    /** Refreshes the strips even under a modal dialog or progress, which the IDE may show while it reopens projects. */
    public fun notifyChanged() {
        val application: Application = ApplicationManager.getApplication()
        application.invokeLater(
            { application.messageBus.syncPublisher(WindowTabsListener.TOPIC).tabsChanged() },
            ModalityState.any(),
        )
    }

    /** Puts the target frame where the current one is and brings it to the front, so switching feels like a tab. */
    public fun switchTo(
        current: Project,
        target: Project,
    ) {
        placeOver(current, target)
        focusWindow(target)
    }

    /** A target already maximized where it belongs stays put: new bounds would overwrite its restore size. */
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

        val sameScreen: Boolean = source.graphicsConfiguration.device == destination.graphicsConfiguration.device

        if (FramePlacement.alreadyInPlace(source.extendedState, destination.extendedState, sameScreen)) {
            return
        }

        destination.extendedState = Frame.NORMAL
        val placement: FramePlacement = FramePlacement.matching(source.extendedState, source.bounds, destination.size)
        destination.bounds = placement.bounds
        destination.extendedState = placement.extendedState
    }

    /** Brings the Welcome screen back once no project is left, as the platform's own close actions do. */
    public fun close(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) {
                return@invokeLater
            }

            ProjectManager.getInstance().closeAndDispose(project)
            WelcomeFrame.showIfNoProjectOpened()
        }
    }

    public companion object {
        /** No new project window for this long means the IDE is done reopening the last session. */
        private const val REOPEN_QUIET_SECONDS: Long = 30L

        public fun getInstance(): WindowTabsService = service()

        internal fun savedOrder(): TabOrder =
            TabOrder(WindowTabsSettings.getInstance().state.tabOrder) { keys: List<String> ->
                WindowTabsSettings.getInstance().state.tabOrder = keys.toMutableList()
            }
    }
}
