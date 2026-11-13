package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeRootPaneNorthExtension
import javax.swing.JComponent

public class WindowTabsNorthExtension : IdeRootPaneNorthExtension {
    override val key: String = "WindowTabs"

    override fun createComponent(
        project: Project,
        isDocked: Boolean,
    ): JComponent = WindowTabsBar(project)
}
