package br.dev.ddr.windowtabs

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent

class StepTabActionTest : WindowTabsTestCase() {
    private fun action(actionId: String): AnAction = ActionManager.getInstance().getAction(actionId)

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

    fun `test next and previous walk the strip in both directions`() {
        val second = openAnotherProject("second")
        val third = openAnotherProject("third")
        fakeWindowManager().withFrames(project, second, third)

        action(StepTabAction.NEXT_ID).actionPerformed(event(action(StepTabAction.NEXT_ID)))
        action(StepTabAction.PREVIOUS_ID).actionPerformed(event(action(StepTabAction.PREVIOUS_ID)))

        assertEquals(listOf(second, third), focused)
    }

    fun `test does nothing without a project`() {
        openAnotherProject("other")
        fakeWindowManager()

        action(StepTabAction.NEXT_ID).actionPerformed(event(action(StepTabAction.NEXT_ID), withProject = false))

        assertEmpty(focused)
    }

    fun `test is enabled only with a project and updates off the event thread`() {
        val next = action(StepTabAction.NEXT_ID)
        val withProject = event(next)
        next.update(withProject)
        assertTrue(withProject.presentation.isEnabled)

        val withoutProject = event(next, withProject = false)
        next.update(withoutProject)
        assertFalse(withoutProject.presentation.isEnabled)

        assertEquals(ActionUpdateThread.BGT, next.actionUpdateThread)
    }
}
