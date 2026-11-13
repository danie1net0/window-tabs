package br.dev.ddr.windowtabs

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project

public class StepTabAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project

        if (project == null) {
            return
        }

        var step = 1

        if (ActionManager.getInstance().getId(this) == PREVIOUS_ID) {
            step = -1
        }

        TabNavigator.stepTab(project, step)
    }

    public companion object {
        public const val NEXT_ID: String = "WindowTabs.NextTab"
        public const val PREVIOUS_ID: String = "WindowTabs.PreviousTab"
    }
}
