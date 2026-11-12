package br.dev.ddr.windowtabs

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/**
 * Goes to the tab whose number ends the action id (WindowTabs.SelectTab1 … WindowTabs.SelectTab9),
 * so plugin.xml registers one class nine times instead of nine classes.
 */
class SelectTabAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val actionId = ActionManager.getInstance().getId(this)
        val number = actionId?.let { registeredId -> registeredId.removePrefix(ID_PREFIX).toIntOrNull() }

        if (project == null || number == null) {
            return
        }

        TabNavigator.selectTab(project, number - 1)
    }

    companion object {
        const val ID_PREFIX = "WindowTabs.SelectTab"
    }
}
