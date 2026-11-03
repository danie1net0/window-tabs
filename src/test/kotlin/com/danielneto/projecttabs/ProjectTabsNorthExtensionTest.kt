package com.danielneto.projecttabs

class ProjectTabsNorthExtensionTest : ProjectTabsTestCase() {
    fun `test adds a tab strip for the frame's project`() {
        val extension = ProjectTabsNorthExtension()

        assertEquals("ProjectTabs", extension.key)
        assertInstanceOf(extension.createComponent(project, false), ProjectTabsBar::class.java)
    }
}
