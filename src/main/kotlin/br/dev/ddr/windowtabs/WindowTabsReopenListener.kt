package br.dev.ddr.windowtabs

import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.project.Project

/** The IDE is done reopening the last session, so the placeholders of what never opened go away. */
class WindowTabsReopenListener : RecentProjectsManager.LastProjectsListener {
    override fun lastProjectsReopened(activeProject: Project) {
        WindowTabsService.getInstance().reopenFinished()
    }
}
