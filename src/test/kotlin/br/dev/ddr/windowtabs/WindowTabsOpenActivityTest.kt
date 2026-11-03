package br.dev.ddr.windowtabs

import kotlinx.coroutines.runBlocking

class WindowTabsOpenActivityTest : WindowTabsTestCase() {
    fun `test opening a project refreshes the tabs`() {
        val calls = recordTabsChanged()

        runBlocking { WindowTabsOpenActivity().execute(project) }
        dispatchEvents()

        assertEquals(1, calls())
    }
}
