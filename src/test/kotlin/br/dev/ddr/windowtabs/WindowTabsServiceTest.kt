package br.dev.ddr.windowtabs

import com.intellij.testFramework.PlatformTestUtil
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Frame
import java.awt.Rectangle
import java.nio.file.Files

class WindowTabsServiceTest : WindowTabsTestCase() {
    private val service get() = WindowTabsService.getInstance()

    fun `test lists open projects in the order they were opened`() {
        val second = openAnotherProject("second")
        val third = openAnotherProject("third")

        assertEquals(listOf(project, second, third), openProjects())
    }

    fun `test forgets closed projects`() {
        val second = openAnotherProject("second")
        assertEquals(listOf(project, second), openProjects())

        service.close(second)
        dispatchEvents()

        assertTrue(second.isDisposed)
        assertEquals(listOf(project), openProjects())
    }

    fun `test closing a project that is already gone does nothing`() {
        val gone = closedProject()

        service.close(gone)
        dispatchEvents()

        assertEquals(listOf(project), openProjects())
    }

    fun `test shows the projects still reopening after the open ones`() {
        service.expectReopening(listOf(WindowTab.Reopening("/work/b", "b"), WindowTab.Reopening("/work/c", "c")))

        val tabs = service.tabs(project)

        assertEquals(listOf(project.basePath, "/work/b", "/work/c"), tabs.map { tab: WindowTab -> tab.key })
        assertInstanceOf(tabs[1], WindowTab.Reopening::class.java)
    }

    fun `test a reopened project takes its placeholder's place`() {
        val directory = Files.createTempDirectory("late")
        val path = directory.toString()
        service.expectReopening(listOf(WindowTab.Reopening(path, "late"), WindowTab.Reopening("/work/last", "last")))
        service.tabs(project)

        Files.createDirectories(directory.resolve(".idea"))
        val late = PlatformTestUtil.loadAndOpenProject(directory, testRootDisposable)
        val tabs = service.tabs(project)

        assertEquals(listOf(project.basePath, late.basePath, "/work/last"), tabs.map { tab: WindowTab -> tab.key })
        assertSame(late, (tabs[1] as WindowTab.Open).project)
    }

    fun `test the window's own project counts as open before ProjectManager lists it`() {
        val gone = closedProject()
        service.expectReopening(listOf(WindowTab.Reopening(gone.basePath!!, "closed")))

        val tabs = service.tabs(gone)

        assertEquals(listOf(project, gone), tabs.map { tab: WindowTab -> (tab as WindowTab.Open).project })
    }

    fun `test finishing the reopen drops what never opened and refreshes`() {
        service.expectReopening(listOf(WindowTab.Reopening("/work/missing", "missing")))
        val calls = recordTabsChanged()

        service.reopenFinished()
        dispatchEvents()

        assertEquals(listOf(project), openProjects())
        assertEquals(1, service.tabs(project).size)
        assertEquals(1, calls())
    }

    fun `test notifies listeners on the event thread`() {
        val calls = recordTabsChanged()

        service.notifyChanged()
        assertEquals(0, calls())

        dispatchEvents()
        assertEquals(1, calls())
    }

    fun `test switching moves the target frame over a normal current frame`() {
        val target = openAnotherProject("target")
        val windowManager = fakeWindowManager()
        val source = fakeFrame(Frame.NORMAL, Rectangle(10, 20, 1000, 700))
        val destination = fakeFrame()
        `when`(windowManager.getFrame(project)).thenReturn(source)
        `when`(windowManager.getFrame(target)).thenReturn(destination)

        service.switchTo(project, target)

        verify(destination).bounds = Rectangle(10, 20, 1000, 700)
        verify(destination, times(2)).extendedState = Frame.NORMAL
        assertEquals(listOf(target), focused)
    }

    fun `test switching keeps the target maximized when the current frame is`() {
        val target = openAnotherProject("target")
        val windowManager = fakeWindowManager()
        val source = fakeFrame(Frame.MAXIMIZED_BOTH)
        val destination = fakeFrame()
        `when`(windowManager.getFrame(project)).thenReturn(source)
        `when`(windowManager.getFrame(target)).thenReturn(destination)

        service.switchTo(project, target)

        verify(destination, never()).bounds = any()
        verify(destination).extendedState = Frame.MAXIMIZED_BOTH
    }

    fun `test switching without the current frame leaves the target where it is`() {
        val target = openAnotherProject("target")
        val windowManager = fakeWindowManager()
        val destination = fakeFrame()
        `when`(windowManager.getFrame(target)).thenReturn(destination)

        service.switchTo(project, target)

        verify(destination, never()).extendedState = anyInt()
        assertEquals(listOf(target), focused)
    }

    fun `test switching without the target frame touches nothing`() {
        val target = openAnotherProject("target")
        val windowManager = fakeWindowManager()
        val source = fakeFrame()
        `when`(windowManager.getFrame(project)).thenReturn(source)

        service.switchTo(project, target)

        verify(source, never()).extendedState
    }

    fun `test the real focus leaves a project without a window alone`() {
        val target = openAnotherProject("target")

        service.switchTo(project, target)

        assertFalse(target.isDisposed)
    }

    fun `test moving a tab changes the order and refreshes`() {
        val second = openAnotherProject("second")
        val calls = recordTabsChanged()

        service.move(second.basePath!!, 0)
        dispatchEvents()

        assertEquals(listOf(second, project), openProjects())
        assertEquals(1, calls())
    }

    fun `test the strip follows the settings and the OS`() {
        assertFalse(service.showsStrip(1))
        assertTrue(service.showsStrip(2))

        service.isMac = true
        assertFalse(service.showsStrip(2))
    }
}
