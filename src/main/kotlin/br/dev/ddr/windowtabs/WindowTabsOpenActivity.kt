package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Refreshes every strip once a project's window is up, and pushes back the end of the reopen. */
class WindowTabsOpenActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        WindowTabsService.getInstance().projectOpened()
    }
}
