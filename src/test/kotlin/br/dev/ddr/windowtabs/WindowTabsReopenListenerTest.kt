package br.dev.ddr.windowtabs

class WindowTabsReopenListenerTest : WindowTabsTestCase() {
    fun `test the end of the reopen drops the placeholders`() {
        WindowTabsService.getInstance().expectReopening(listOf(WindowTab.Reopening("/work/missing", "missing")))
        val calls = recordTabsChanged()

        WindowTabsReopenListener().lastProjectsReopened(project)
        dispatchEvents()

        assertEquals(1, WindowTabsService.getInstance().tabs(project).size)
        assertEquals(1, calls())
    }
}
