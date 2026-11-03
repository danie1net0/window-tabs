package br.dev.ddr.windowtabs

import kotlinx.coroutines.runBlocking

class WindowTabsLifecycleTest : WindowTabsTestCase() {
    fun `test opening a project refreshes the tabs`() {
        val calls = recordTabsChanged()

        runBlocking { WindowTabsOpenActivity().execute(project) }
        dispatchEvents()

        assertEquals(1, calls())
    }

    fun `test closing a project refreshes the tabs`() {
        val calls = recordTabsChanged()

        WindowTabsCloseListener().projectClosed(project)
        dispatchEvents()

        assertEquals(1, calls())
    }

    fun `test the end of the reopen drops the placeholders`() {
        WindowTabsService.getInstance().expectReopening(listOf(WindowTab.Reopening("/work/missing", "missing")))
        val calls = recordTabsChanged()

        WindowTabsReopenListener().lastProjectsReopened(project)
        dispatchEvents()

        assertEquals(1, WindowTabsService.getInstance().tabs(project).size)
        assertEquals(1, calls())
    }
}
