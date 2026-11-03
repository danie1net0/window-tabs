package br.dev.ddr.windowtabs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.replaceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.withSettings
import java.awt.Frame
import java.awt.Rectangle
import java.nio.file.Files
import javax.swing.JFrame

/** Real projects, so the tests see what ProjectManager really reports, plus a fake WindowManager for the frames. */
abstract class WindowTabsTestCase : HeavyPlatformTestCase() {
    /** Projects whose window the code under test asked to raise, once [fakeWindowManager] is in place. */
    protected val focused = mutableListOf<Project>()

    /** The service outlives each test, so nothing a test left waiting may leak into the next one. */
    override fun setUp() {
        super.setUp()
        WindowTabsService.getInstance().expectReopening(emptyList())
    }

    protected fun openProjects(current: Project = project): List<Project> =
        WindowTabsService
            .getInstance()
            .tabs(current)
            .filterIsInstance<WindowTab.Open>()
            .map { it.project }

    protected fun openAnotherProject(name: String): Project {
        val directory = Files.createTempDirectory(name)
        Files.createDirectories(directory.resolve(".idea"))
        Files.writeString(directory.resolve(".idea/.name"), name)

        return PlatformTestUtil.loadAndOpenProject(directory, testRootDisposable)
    }

    /** A project that was open and is now closed, for the code paths that must ignore disposed projects. */
    protected fun closedProject(): Project {
        val project = openAnotherProject("closed")
        WindowTabsService.getInstance().close(project)
        dispatchEvents()

        return project
    }

    /** ProjectUtil.focusProjectWindow casts the frame to IdeFrame, so the fake must be both. */
    protected fun fakeFrame(
        state: Int = Frame.NORMAL,
        bounds: Rectangle = Rectangle(0, 0, 800, 600),
    ): JFrame {
        val frame = mock(JFrame::class.java, withSettings().extraInterfaces(IdeFrame::class.java))
        `when`(frame.extendedState).thenReturn(state)
        `when`(frame.bounds).thenReturn(bounds)

        return frame
    }

    protected fun fakeWindowManager(): WindowManager {
        val windowManager = mock(WindowManager::class.java)
        val disposable = Disposer.newDisposable()
        Disposer.register(testRootDisposable, disposable)
        ApplicationManager.getApplication().replaceService(WindowManager::class.java, windowManager, disposable)

        val service = WindowTabsService.getInstance()
        val realFocus = service.focusWindow
        service.focusWindow = { focused.add(it) }
        Disposer.register(disposable) { service.focusWindow = realFocus }

        return windowManager
    }

    protected fun recordTabsChanged(): () -> Int {
        var calls = 0
        ApplicationManager
            .getApplication()
            .messageBus
            .connect(testRootDisposable)
            .subscribe(WindowTabsListener.TOPIC, WindowTabsListener { calls++ })

        return { calls }
    }

    protected fun dispatchEvents() = PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
}
