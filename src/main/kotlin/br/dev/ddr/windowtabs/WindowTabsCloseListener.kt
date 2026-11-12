package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class WindowTabsCloseListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        WindowTabsService.getInstance().notifyChanged()
    }
}
