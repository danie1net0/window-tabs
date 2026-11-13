package br.dev.ddr.windowtabs

import com.intellij.ide.GeneralSettings
import com.intellij.ide.RecentProjectMetaInfo
import com.intellij.ide.RecentProjectsManagerBase

/**
 * The IDE reopens the last session's projects one window at a time, which takes a while with several of them.
 * These are the ones still on their way, so the strip can show every tab from the first window on.
 */
public class ReopeningProjects {
    private val waiting = mutableListOf<WindowTab.Reopening>()

    public fun expect(projects: List<WindowTab.Reopening>) {
        waiting.clear()
        waiting.addAll(projects)
    }

    /** Forgets, for good, the ones whose window is open now: closing that project later leaves no placeholder. */
    public fun remaining(openKeys: Set<String>): List<WindowTab.Reopening> {
        waiting.removeAll { project: WindowTab.Reopening -> project.key in openKeys }

        return waiting.toList()
    }

    public fun finish() {
        waiting.clear()
    }

    public companion object {
        public fun fromLastSession(): List<WindowTab.Reopening> {
            if (!GeneralSettings.getInstance().isReopenLastProject) {
                return emptyList()
            }

            val recentProjects: RecentProjectsManagerBase = RecentProjectsManagerBase.getInstanceEx()

            return recentProjects.state.additionalInfo.entries
                .filter { (_, metadata: RecentProjectMetaInfo) -> metadata.opened && !metadata.hidden }
                .sortedBy { (_, metadata: RecentProjectMetaInfo) -> metadata.projectOpenTimestamp }
                .map { (path: String, metadata: RecentProjectMetaInfo) ->
                    WindowTab.Reopening(path, metadata.displayName ?: recentProjects.getProjectName(path))
                }
        }
    }
}
