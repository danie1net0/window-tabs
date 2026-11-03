package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project

/** What a tab in the strip stands for. */
sealed interface WindowTab {
    /** Identifies the tab across refreshes, so a placeholder keeps its place when its project opens. */
    val key: String

    class Open(
        val project: Project,
    ) : WindowTab {
        // basePath is only null for the default project, which never has a window.
        override val key = project.basePath.toString()
    }

    /** A project the IDE is still reopening from the last session; it becomes an [Open] tab once its window is up. */
    class Reopening(
        val path: String,
        val name: String,
    ) : WindowTab {
        override val key = path
    }
}
