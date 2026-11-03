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
}
