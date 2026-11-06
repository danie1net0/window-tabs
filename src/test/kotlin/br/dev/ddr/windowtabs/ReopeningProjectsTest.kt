package br.dev.ddr.windowtabs

import com.intellij.ide.GeneralSettings
import com.intellij.ide.RecentProjectMetaInfo
import com.intellij.ide.RecentProjectsManagerBase

class ReopeningProjectsTest : WindowTabsTestCase() {
    private val alpha = WindowTab.Reopening("/work/alpha", "alpha")
    private val beta = WindowTab.Reopening("/work/beta", "beta")

    fun `test waits for every expected project`() {
        val reopening = ReopeningProjects()
        reopening.expect(listOf(alpha, beta))

        assertEquals(listOf(alpha, beta), reopening.remaining(emptySet()))
    }

    fun `test forgets an opened project even after it closes again`() {
        val reopening = ReopeningProjects()
        reopening.expect(listOf(alpha, beta))

        assertEquals(listOf(beta), reopening.remaining(setOf("/work/alpha")))
        assertEquals(listOf(beta), reopening.remaining(emptySet()))
    }

    fun `test expecting again replaces the old list`() {
        val reopening = ReopeningProjects()
        reopening.expect(listOf(alpha))
        reopening.expect(listOf(beta))

        assertEquals(listOf(beta), reopening.remaining(emptySet()))
    }

    fun `test finishing drops everything still waiting`() {
        val reopening = ReopeningProjects()
        reopening.expect(listOf(alpha, beta))

        reopening.finish()

        assertEmpty(reopening.remaining(emptySet()))
    }

    fun `test the last session's open projects, oldest first, skipping closed and hidden ones`() {
        withLastSession(
            "/work/second" to projectInfo(opened = true, timestamp = 2, displayName = "Second"),
            "/work/first" to projectInfo(opened = true, timestamp = 1),
            "/work/closed" to projectInfo(opened = false, timestamp = 3),
            "/work/hidden" to projectInfo(opened = true, timestamp = 4, hidden = true),
        ) {
            val projects = ReopeningProjects.fromLastSession()

            assertEquals(listOf("/work/first", "/work/second"), projects.map { project -> project.path })
            assertEquals(listOf("first", "Second"), projects.map { project -> project.name })
        }
    }

    fun `test nothing to wait for when the IDE does not reopen projects`() {
        withLastSession("/work/first" to projectInfo(opened = true, timestamp = 1), reopen = false) {
            assertEmpty(ReopeningProjects.fromLastSession())
        }
    }

    private fun projectInfo(
        opened: Boolean,
        timestamp: Long,
        hidden: Boolean = false,
        displayName: String? = null,
    ): RecentProjectMetaInfo {
        val projectInfo = RecentProjectMetaInfo()
        projectInfo.opened = opened
        projectInfo.hidden = hidden
        projectInfo.projectOpenTimestamp = timestamp
        projectInfo.displayName = displayName

        return projectInfo
    }

    /** Swaps the recent-projects state and the reopen setting for the length of [block]. */
    private fun withLastSession(
        vararg entries: Pair<String, RecentProjectMetaInfo>,
        reopen: Boolean = true,
        block: () -> Unit,
    ) {
        val settings = GeneralSettings.getInstance()
        val savedReopen = settings.isReopenLastProject
        val additionalInfo = RecentProjectsManagerBase.getInstanceEx().state.additionalInfo
        val saved = additionalInfo.toMap()
        settings.isReopenLastProject = reopen
        additionalInfo.clear()
        additionalInfo.putAll(entries)
        try {
            block()
        } finally {
            additionalInfo.clear()
            additionalInfo.putAll(saved)
            settings.isReopenLastProject = savedReopen
        }
    }
}
