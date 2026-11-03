package br.dev.ddr.windowtabs

class WindowTabTest : WindowTabsTestCase() {
    fun `test an open tab is keyed by its project's path`() {
        val tab = WindowTab.Open(project)

        assertSame(project, tab.project)
        assertEquals(project.basePath, tab.key)
    }

    fun `test a reopening tab is keyed by the path it will open`() {
        val tab = WindowTab.Reopening("/work/later", "later")

        assertEquals("/work/later", tab.key)
        assertEquals("/work/later", tab.path)
        assertEquals("later", tab.name)
    }
}
