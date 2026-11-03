package br.dev.ddr.windowtabs

import com.intellij.ide.GeneralSettings
import com.intellij.ide.RecentProjectMetaInfo
import com.intellij.ide.RecentProjectsManagerBase

class ReopeningProjectsTest : WindowTabsTestCase() {
    private val a = WindowTab.Reopening("/work/a", "a")
    private val b = WindowTab.Reopening("/work/b", "b")

    fun `test waits for every expected project`() {
        val reopening = ReopeningProjects()
        reopening.expect(listOf(a, b))

        assertEquals(listOf(a, b), reopening.remaining(emptySet()))
    }

    fun `test forgets an opened project even after it closes again`() {
        val reopening = ReopeningProjects()
        reopening.expect(listOf(a, b))

        assertEquals(listOf(b), reopening.remaining(setOf("/work/a")))
        assertEquals(listOf(b), reopening.remaining(emptySet()))
    }

    fun `test expecting again replaces the old list`() {
        val reopening = ReopeningProjects()
        reopening.expect(listOf(a))
        reopening.expect(listOf(b))

        assertEquals(listOf(b), reopening.remaining(emptySet()))
    }

    fun `test finishing drops everything still waiting`() {
        val reopening = ReopeningProjects()
        reopening.expect(listOf(a, b))

        reopening.finish()

        assertEmpty(reopening.remaining(emptySet()))
    }

    fun `test the last session's open projects, oldest first, skipping closed and hidden ones`() {
        withLastSession(
            "/work/second" to meta(opened = true, timestamp = 2, displayName = "Second"),
            "/work/first" to meta(opened = true, timestamp = 1),
            "/work/closed" to meta(opened = false, timestamp = 3),
            "/work/hidden" to meta(opened = true, timestamp = 4, hidden = true),
        ) {
            val projects = ReopeningProjects.fromLastSession()

            assertEquals(listOf("/work/first", "/work/second"), projects.map { it.path })
            assertEquals(listOf("first", "Second"), projects.map { it.name })
        }
    }

    fun `test nothing to wait for when the IDE does not reopen projects`() {
        withLastSession("/work/first" to meta(opened = true, timestamp = 1), reopen = false) {
            assertEmpty(ReopeningProjects.fromLastSession())
        }
    }

    private fun meta(
        opened: Boolean,
        timestamp: Long,
        hidden: Boolean = false,
        displayName: String? = null,
    ): RecentProjectMetaInfo {
        val info = RecentProjectMetaInfo()
        info.opened = opened
        info.hidden = hidden
        info.projectOpenTimestamp = timestamp
        info.displayName = displayName

        return info
    }

    /** Swaps the recent-projects state and the reopen setting for the length of [block]. */
    private fun withLastSession(
        vararg entries: Pair<String, RecentProjectMetaInfo>,
        reopen: Boolean = true,
        block: () -> Unit,
    ) {
        val settings = GeneralSettings.getInstance()
        val savedReopen = settings.isReopenLastProject
        val info = RecentProjectsManagerBase.getInstanceEx().state.additionalInfo
        val saved = info.toMap()
        settings.isReopenLastProject = reopen
        info.clear()
        info.putAll(entries)
        try {
            block()
        } finally {
            info.clear()
            info.putAll(saved)
            settings.isReopenLastProject = savedReopen
        }
    }
}
