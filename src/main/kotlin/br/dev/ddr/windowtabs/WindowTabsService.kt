package br.dev.ddr.windowtabs

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import java.awt.Frame

@Service(Service.Level.APP)
class WindowTabsService {
    private val order = TabOrder()

    /** Raises a project's window; tests swap it out because the real one needs real frames. */
    internal var focusWindow: (Project) -> Unit = { ProjectUtil.focusProjectWindow(it, true) }

    fun projects(): List<Project> {
        val open = ProjectManager.getInstance().openProjects
        val byKey = open.associateBy { it.locationHash }

        return order.sync(open.map { it.locationHash }).map { byKey.getValue(it) }
    }

    fun notifyChanged() {
        val application = ApplicationManager.getApplication()
        application.invokeLater {
            application.messageBus.syncPublisher(WindowTabsListener.TOPIC).tabsChanged()
        }
    }

    /** Puts the target frame where the current one is and brings it to the front, so switching feels like a tab. */
    fun switchTo(
        current: Project,
        target: Project,
    ) {
        val windowManager = WindowManager.getInstance()
        val source = windowManager.getFrame(current)
        val destination = windowManager.getFrame(target)

        if (source != null && destination != null) {
            val placement = FramePlacement.matching(source.extendedState, source.bounds)
            destination.extendedState = Frame.NORMAL
            placement.bounds?.let { destination.bounds = it }
            destination.extendedState = placement.extendedState
        }

        focusWindow(target)
    }

    fun close(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                ProjectManager.getInstance().closeAndDispose(project)
            }
        }
    }

    companion object {
        fun getInstance(): WindowTabsService = service()
    }
}
