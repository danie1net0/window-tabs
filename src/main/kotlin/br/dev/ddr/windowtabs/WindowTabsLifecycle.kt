package br.dev.ddr.windowtabs

import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity

class WindowTabsOpenActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        WindowTabsService.getInstance().notifyChanged()
    }
}

class WindowTabsCloseListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        WindowTabsService.getInstance().notifyChanged()
    }
}

class WindowTabsReopenListener : RecentProjectsManager.LastProjectsListener {
    override fun lastProjectsReopened(activeProject: Project) {
        WindowTabsService.getInstance().reopenFinished()
    }
}
