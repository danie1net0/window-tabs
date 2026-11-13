package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

public class WindowTabsOpenActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        WindowTabsService.getInstance().projectOpened()
    }
}
