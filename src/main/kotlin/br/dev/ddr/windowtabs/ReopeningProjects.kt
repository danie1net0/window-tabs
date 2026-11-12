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

    fun finish() {
        waiting.clear()
    }

    companion object {
        fun fromLastSession(): List<WindowTab.Reopening> {
            if (!GeneralSettings.getInstance().isReopenLastProject) {
                return emptyList()
            }

            val recentProjects = RecentProjectsManagerBase.getInstanceEx()

            return recentProjects.state.additionalInfo.entries
                .filter { (_, metadata) -> metadata.opened && !metadata.hidden }
                .sortedBy { (_, metadata) -> metadata.projectOpenTimestamp }
                .map { (path, metadata) ->
                    WindowTab.Reopening(path, metadata.displayName ?: recentProjects.getProjectName(path))
                }
        }
    }
}
