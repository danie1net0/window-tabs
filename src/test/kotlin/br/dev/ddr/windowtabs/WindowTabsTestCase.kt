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

    /** The service and the settings outlive each test, so nothing a test left behind may leak into the next one. */
    override fun setUp() {
        super.setUp()
        WindowTabsSettings.getInstance().loadState(WindowTabsSettings.Options())
        WindowTabsService.getInstance().expectReopening(emptyList())
        WindowTabsService.getInstance().isMac = false
    }

    protected fun openProjects(current: Project = project): List<Project> =
        WindowTabsService
            .getInstance()
            .tabs(current)
            .filterIsInstance<WindowTab.Open>()
            .map { tab: WindowTab.Open -> tab.project }

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

    /** Gives each of [projects] a normal fake frame; the frames exist before stubbing, which Mockito cannot nest. */
    protected fun WindowManager.withFrames(vararg projects: Project) {
        val frames = projects.map { project: Project -> project to fakeFrame() }
        frames.forEach { (project: Project, frame: JFrame) -> `when`(getFrame(project)).thenReturn(frame) }
    }

    protected fun fakeWindowManager(): WindowManager {
        val windowManager = mock(WindowManager::class.java)
        val disposable = Disposer.newDisposable()
        Disposer.register(testRootDisposable, disposable)
        ApplicationManager.getApplication().replaceService(WindowManager::class.java, windowManager, disposable)

        val service = WindowTabsService.getInstance()
        val realFocus = service.focusWindow
        service.focusWindow = { project: Project -> focused.add(project) }
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

    protected fun dispatchEvents(): Unit = PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
}
