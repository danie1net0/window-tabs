package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeRootPaneNorthExtension
import javax.swing.JComponent

/** Adds the tab strip across the top of every project frame, under the main toolbar. */
class WindowTabsNorthExtension : IdeRootPaneNorthExtension {
    override val key: String = "WindowTabs"

    override fun createComponent(
        project: Project,
        isDocked: Boolean,
    ): JComponent = WindowTabsBar(project)
}
