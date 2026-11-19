package br.dev.ddr.windowtabs

class WindowTabsSettingsTest : WindowTabsTestCase() {
    fun `test defaults keep the strip as it was before the settings existed`() {
        val options = WindowTabsSettings.getInstance().state

        assertFalse(options.showWithSingleProject)
        assertTrue(options.showProjectIcons)
        assertFalse(options.showOnMac)
        assertEmpty(options.tabOrder)
    }

    fun `test the settings are one application-wide instance`() {
        assertSame(WindowTabsSettings.getInstance(), WindowTabsSettings.getInstance())
    }

    fun `test a dragged order is saved for the next start`() {
        val other = openAnotherProject("other")

        WindowTabsService.getInstance().move(keyOf(other), 0)

        assertEquals(listOf(keyOf(other), keyOf(project)), WindowTabsSettings.getInstance().state.tabOrder)
    }
}
