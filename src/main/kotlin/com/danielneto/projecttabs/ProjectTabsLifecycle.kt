package com.danielneto.projecttabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity

class ProjectTabsOpenActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ProjectTabsService.getInstance().notifyChanged()
    }
}

class ProjectTabsCloseListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        ProjectTabsService.getInstance().notifyChanged()
    }
}
