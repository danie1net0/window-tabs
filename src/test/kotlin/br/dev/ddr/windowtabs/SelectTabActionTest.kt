package br.dev.ddr.windowtabs

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent

class SelectTabActionTest : WindowTabsTestCase() {
    private fun action(number: Int): AnAction {
        val actionId: String = "${SelectTabAction.ID_PREFIX}$number"

        return ActionManager.getInstance().getAction(actionId)
    }

    private fun event(
        action: AnAction,
        withProject: Boolean = true,
    ): AnActionEvent {
        val context = SimpleDataContext.builder()

        if (withProject) {
            context.add(CommonDataKeys.PROJECT, project)
        }

        return TestActionEvent.createTestEvent(action, context.build())
    }

    fun `test registers one action per tab number`() {
        (1..9).forEach { number: Int -> assertInstanceOf(action(number), SelectTabAction::class.java) }
    }

    fun `test goes to the tab with the action's number`() {
        val other = openAnotherProject("other")
        fakeWindowManager().withFrames(project, other)

        action(2).actionPerformed(event(action(2)))

        assertEquals(listOf(other), focused)
    }

    fun `test does nothing without a project or a registered id`() {
        openAnotherProject("other")
        fakeWindowManager()

        action(2).actionPerformed(event(action(2), withProject = false))
        val unregistered = SelectTabAction()
        unregistered.actionPerformed(event(unregistered))

        assertEmpty(focused)
    }

    fun `test is enabled only with a project`() {
        val withProject = event(action(1))
        action(1).update(withProject)
        assertTrue(withProject.presentation.isEnabled)

        val withoutProject = event(action(1), withProject = false)
        action(1).update(withoutProject)
        assertFalse(withoutProject.presentation.isEnabled)
    }

    fun `test updates off the event thread`() {
        assertEquals(ActionUpdateThread.BGT, action(1).actionUpdateThread)
    }
}
