package br.dev.ddr.windowtabs

class WindowTabsCloseListenerTest : WindowTabsTestCase() {
    fun `test closing a project refreshes the tabs`() {
        val calls = recordTabsChanged()

        WindowTabsCloseListener().projectClosed(project)
        dispatchEvents()

        assertEquals(1, calls())
    }
}
