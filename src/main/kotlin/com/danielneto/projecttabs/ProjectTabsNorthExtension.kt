package com.danielneto.projecttabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeRootPaneNorthExtension
import javax.swing.JComponent

/** Adds the tab strip across the top of every project frame, under the main toolbar. */
class ProjectTabsNorthExtension : IdeRootPaneNorthExtension {
    override val key: String = "ProjectTabs"

    override fun createComponent(
        project: Project,
        isDocked: Boolean,
    ): JComponent = ProjectTabsBar(project)
}
