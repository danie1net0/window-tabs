package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project

/** Keyboard navigation over the strip: straight to a tab by number, or one tab over. */
object TabNavigator {
    private val service get() = WindowTabsService.getInstance()

    /** Switches from [current] to the tab at [index] (0-based); a placeholder or a missing tab does nothing. */
    fun selectTab(
        current: Project,
        index: Int,
    ) {
        val target = service.tabs(current).getOrNull(index)

        if (target !is WindowTab.Open || target.project == current) {
            return
        }

        service.switchTo(current, target.project)
    }

    /** Switches to the next ([step] = 1) or previous ([step] = -1) open project, wrapping around the strip. */
    fun stepTab(
        current: Project,
        step: Int,
    ) {
        val open = service.tabs(current).filterIsInstance<WindowTab.Open>().map { it.project }

        if (open.size < 2) {
            return
        }

        val index = open.indexOf(current)
        val target = open[Math.floorMod(index + step, open.size)]
        service.switchTo(current, target)
    }
}
