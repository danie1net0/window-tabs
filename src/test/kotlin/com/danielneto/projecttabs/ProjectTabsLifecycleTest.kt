package com.danielneto.projecttabs

import kotlinx.coroutines.runBlocking

class ProjectTabsLifecycleTest : ProjectTabsTestCase() {
    fun `test opening a project refreshes the tabs`() {
        val calls = recordTabsChanged()

        runBlocking { ProjectTabsOpenActivity().execute(project) }
        dispatchEvents()

        assertEquals(1, calls())
    }

    fun `test closing a project refreshes the tabs`() {
        val calls = recordTabsChanged()

        ProjectTabsCloseListener().projectClosed(project)
        dispatchEvents()

        assertEquals(1, calls())
    }
}
