package br.dev.ddr.windowtabs

import com.intellij.ide.GeneralSettings
import com.intellij.ide.RecentProjectsManagerBase

/**
 * The IDE reopens the last session's projects one window at a time, which takes a while with several of them.
 * These are the ones still on their way, so the strip can show every tab from the first window on.
 */
class ReopeningProjects {
    private val waiting = mutableListOf<WindowTab.Reopening>()

    fun expect(projects: List<WindowTab.Reopening>) {
        waiting.clear()
        waiting.addAll(projects)
    }

    /** Forgets, for good, the ones whose window is open now: closing that project later leaves no placeholder. */
    fun remaining(openKeys: Set<String>): List<WindowTab.Reopening> {
        waiting.removeAll { project -> project.key in openKeys }

        return waiting.toList()
    }

    /** The IDE is done reopening; whatever did not open is not coming. */
    fun finish() {
        waiting.clear()
    }

    companion object {
        /** The projects open when the IDE last quit, oldest first, if the IDE is set to reopen them. */
        fun fromLastSession(): List<WindowTab.Reopening> {
            if (!GeneralSettings.getInstance().isReopenLastProject) {
                return emptyList()
            }

            val recent = RecentProjectsManagerBase.getInstanceEx()

            return recent.state.additionalInfo.entries
                .filter { (_, info) -> info.opened && !info.hidden }
                .sortedBy { (_, info) -> info.projectOpenTimestamp }
                .map { (path, info) -> WindowTab.Reopening(path, info.displayName ?: recent.getProjectName(path)) }
        }
    }
}
