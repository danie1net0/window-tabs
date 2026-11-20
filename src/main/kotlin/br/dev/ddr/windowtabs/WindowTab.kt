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
         * The path the IDE keeps for the project among its recent ones, so it matches the last session's entry:
         * the folder, or the .ipr file of a project kept in one. A project without a location falls back to its folder.
         */
        override val key: String =
            FileUtilRt.toSystemIndependentName(
                project.presentableUrl ?: project.basePath.toString(),
            )
    }

    public class Reopening(
        public val path: String,
        public val name: String,
    ) : WindowTab {
        /** Forward slashes, like [Open.key]: on Windows the path may come with backslashes and must still match. */
        override val key: String = FileUtilRt.toSystemIndependentName(path)
    }
}
