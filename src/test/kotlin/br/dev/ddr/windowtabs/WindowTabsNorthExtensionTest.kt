package br.dev.ddr.windowtabs

class WindowTabsNorthExtensionTest : WindowTabsTestCase() {
    fun `test adds a tab strip for the frame's project`() {
        val extension = WindowTabsNorthExtension()

        assertEquals("WindowTabs", extension.key)
        assertInstanceOf(extension.createComponent(project, false), WindowTabsBar::class.java)
    }
}
