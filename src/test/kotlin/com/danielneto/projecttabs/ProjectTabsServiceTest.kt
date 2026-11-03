package com.danielneto.projecttabs

import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Frame
import java.awt.Rectangle

class ProjectTabsServiceTest : ProjectTabsTestCase() {
    private val service get() = ProjectTabsService.getInstance()

    fun `test lists open projects in the order they were opened`() {
        val second = openAnotherProject("second")
        val third = openAnotherProject("third")

        assertEquals(listOf(project, second, third), service.projects())
    }

    fun `test forgets closed projects`() {
        val second = openAnotherProject("second")
        assertEquals(listOf(project, second), service.projects())

        service.close(second)
        dispatchEvents()

        assertTrue(second.isDisposed)
        assertEquals(listOf(project), service.projects())
    }

    fun `test closing a project that is already gone does nothing`() {
        val gone = closedProject()

        service.close(gone)
        dispatchEvents()

        assertEquals(listOf(project), service.projects())
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
        verify(destination, org.mockito.Mockito.times(2)).extendedState = Frame.NORMAL
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

        verify(destination, never()).bounds = org.mockito.ArgumentMatchers.any()
        verify(destination).extendedState = Frame.MAXIMIZED_BOTH
    }

    fun `test switching without the current frame leaves the target where it is`() {
        val target = openAnotherProject("target")
        val windowManager = fakeWindowManager()
        val destination = fakeFrame()
        `when`(windowManager.getFrame(target)).thenReturn(destination)

        service.switchTo(project, target)

        verify(destination, never()).extendedState = org.mockito.ArgumentMatchers.anyInt()
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
}
