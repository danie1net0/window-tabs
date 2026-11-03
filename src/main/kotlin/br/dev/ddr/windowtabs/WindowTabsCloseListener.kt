package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

/** Refreshes every strip once a project is closed, so its tab goes away. */
class WindowTabsCloseListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        WindowTabsService.getInstance().notifyChanged()
    }
}
