package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeRootPaneNorthExtension
import javax.swing.JComponent

public class WindowTabsNorthExtension : IdeRootPaneNorthExtension {
    override val key: String = "WindowTabs"

    /** Only project frames get a strip: a detached editor window is not where a project lives. */
    override fun createComponent(
        project: Project,
        isDocked: Boolean,
    ): JComponent? {
        if (isDocked) {
            return null
        }

        return WindowTabsBar(project)
    }
}
