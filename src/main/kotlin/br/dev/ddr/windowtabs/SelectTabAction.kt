package br.dev.ddr.windowtabs

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project

/**
 * Goes to the tab whose number ends the action id (WindowTabs.SelectTab1 … WindowTabs.SelectTab9),
 * so plugin.xml registers one class nine times instead of nine classes.
 */
public class SelectTabAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        val actionId: String? = ActionManager.getInstance().getId(this)
        val number: Int? = actionId?.let { registeredId: String -> registeredId.removePrefix(ID_PREFIX).toIntOrNull() }

        if (project == null || number == null) {
            return
        }

        TabNavigator.selectTab(project, number - 1)
    }

    public companion object {
        public const val ID_PREFIX: String = "WindowTabs.SelectTab"
    }
}
