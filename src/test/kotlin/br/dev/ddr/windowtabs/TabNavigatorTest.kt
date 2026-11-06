package br.dev.ddr.windowtabs

class TabNavigatorTest : WindowTabsTestCase() {
    fun `test selecting a tab by index switches only to another open project`() {
        val second = openAnotherProject("second")
        WindowTabsService.getInstance().expectReopening(listOf(WindowTab.Reopening("/work/later", "later")))
        fakeWindowManager().withFrames(project, second)

        TabNavigator.selectTab(project, 0)
        TabNavigator.selectTab(project, 2)
        TabNavigator.selectTab(project, 7)
        TabNavigator.selectTab(project, 1)

        assertEquals(listOf(second), focused)
    }

    fun `test stepping needs another open project`() {
        fakeWindowManager()

        TabNavigator.stepTab(project, 1)

        assertEmpty(focused)
    }

    fun `test stepping wraps around the strip`() {
        val second = openAnotherProject("second")
        fakeWindowManager().withFrames(project, second)

        TabNavigator.stepTab(project, -1)
        TabNavigator.stepTab(second, 1)

        assertEquals(listOf(second, project), focused)
    }
}
