package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt

public sealed interface WindowTab {
    /** Identifies the tab across refreshes, so a placeholder keeps its place when its project opens. */
    public val key: String

    public class Open(
        public val project: Project,
    ) : WindowTab {
        /**
         * The project's path, the same string the IDE keeps for it in the last session.
         * Only the default project has no path, and it never gets a window.
         */
        override val key: String = project.basePath.toString()
    }

    public class Reopening(
        public val path: String,
        public val name: String,
    ) : WindowTab {
        /** Forward slashes, like [Open.key]: on Windows the path may come with backslashes and must still match. */
        override val key: String = FileUtilRt.toSystemIndependentName(path)
    }
}
