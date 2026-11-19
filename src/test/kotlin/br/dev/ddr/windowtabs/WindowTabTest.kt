package br.dev.ddr.windowtabs

import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtilRt

class WindowTabTest : WindowTabsTestCase() {
    fun `test an open tab is keyed by the path the IDE keeps for it among recent projects`() {
        val folder = openAnotherProject("folder")

        assertSame(folder, WindowTab.Open(folder).project)
        assertEquals(RecentProjectsManagerBase.getInstanceEx().getProjectPath(folder), WindowTab.Open(folder).key)
    }

    fun `test a project kept in an ipr file is keyed by that file`() {
        val key = WindowTab.Open(project).key

        assertTrue(key.endsWith(".ipr"))
        assertEquals(FileUtilRt.toSystemIndependentName(project.presentableUrl!!), key)
    }

    fun `test a project without a location falls back to its folder`() {
        val defaultProject = ProjectManager.getInstance().defaultProject

        assertEquals(defaultProject.basePath.toString(), WindowTab.Open(defaultProject).key)
    }

    fun `test a reopening tab is keyed by the path it will open`() {
        val tab = WindowTab.Reopening("/work/later", "later")

        assertEquals("/work/later", tab.key)
        assertEquals("/work/later", tab.path)
        assertEquals("later", tab.name)
    }

    fun `test a Windows path gets the same key as the open project's path`() {
        val tab = WindowTab.Reopening("C:\\work\\later", "later")

        assertEquals("C:/work/later", tab.key)
        assertEquals("C:\\work\\later", tab.path)
    }
}
