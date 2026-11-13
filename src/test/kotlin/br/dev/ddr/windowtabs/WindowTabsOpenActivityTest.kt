package br.dev.ddr.windowtabs

import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class WindowTabsOpenActivityTest : WindowTabsTestCase() {
    private val scheduled = mutableListOf<Runnable>()
    private val futures = mutableListOf<Future<*>>()

    override fun setUp() {
        super.setUp()
        val service = WindowTabsService.getInstance()
        val realSchedule = service.scheduleReopenEnd
        service.scheduleReopenEnd = { runnable: Runnable ->
            scheduled.add(runnable)
            CompletableFuture<Unit>().also { future: CompletableFuture<Unit> -> futures.add(future) }
        }
        Disposer.register(testRootDisposable) { service.scheduleReopenEnd = realSchedule }
    }

    fun `test opening a project refreshes the tabs`() {
        val calls = recordTabsChanged()

        runBlocking { WindowTabsOpenActivity().execute(project) }
        dispatchEvents()

        assertEquals(1, calls())
    }

    fun `test the reopen ends once no project has opened for a while`() {
        WindowTabsService.getInstance().expectReopening(listOf(WindowTab.Reopening("/work/missing", "missing")))

        runBlocking { WindowTabsOpenActivity().execute(project) }
        assertEquals(2, WindowTabsService.getInstance().tabs(project).size)

        scheduled.single().run()
        dispatchEvents()

        assertEquals(1, WindowTabsService.getInstance().tabs(project).size)
    }

    fun `test each new window pushes the end of the reopen back`() {
        runBlocking { WindowTabsOpenActivity().execute(project) }
        runBlocking { WindowTabsOpenActivity().execute(project) }

        assertEquals(2, scheduled.size)
        assertTrue(futures.first().isCancelled)
        assertFalse(futures.last().isCancelled)
    }
}
