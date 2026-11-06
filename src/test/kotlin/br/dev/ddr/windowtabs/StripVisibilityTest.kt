package br.dev.ddr.windowtabs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StripVisibilityTest {
    @Test
    fun `needs two tabs by default`() {
        val options = WindowTabsSettings.Options()

        assertFalse(StripVisibility.shows(1, options, isMac = false))
        assertTrue(StripVisibility.shows(2, options, isMac = false))
    }

    @Test
    fun `shows a single tab when asked to`() {
        val options = WindowTabsSettings.Options()
        options.showWithSingleProject = true

        assertFalse(StripVisibility.shows(0, options, isMac = false))
        assertTrue(StripVisibility.shows(1, options, isMac = false))
    }

    @Test
    fun `stays off on macOS unless asked for`() {
        val options = WindowTabsSettings.Options()

        assertFalse(StripVisibility.shows(3, options, isMac = true))

        options.showOnMac = true
        assertTrue(StripVisibility.shows(3, options, isMac = true))
    }
}
