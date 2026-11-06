package br.dev.ddr.windowtabs

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/** Goes to the next tab (WindowTabs.NextTab) or the previous one (WindowTabs.PreviousTab), in the strip's order. */
class StepTabAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project

        if (project == null) {
            return
        }

        var step = 1
        if (ActionManager.getInstance().getId(this) == PREVIOUS_ID) {
            step = -1
        }

        TabNavigator.stepTab(project, step)
    }

    companion object {
        const val NEXT_ID = "WindowTabs.NextTab"
        const val PREVIOUS_ID = "WindowTabs.PreviousTab"
    }
}
