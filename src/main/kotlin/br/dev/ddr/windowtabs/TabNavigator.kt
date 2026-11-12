package br.dev.ddr.windowtabs

import com.intellij.openapi.project.Project

object TabNavigator {
    private val service: WindowTabsService get() = WindowTabsService.getInstance()

    fun selectTab(
        current: Project,
        index: Int,
    ) {
        val target: WindowTab? = service.tabs(current).getOrNull(index)

        if (target !is WindowTab.Open || target.project == current) {
            return
        }

        service.switchTo(current, target.project)
    }

    fun stepTab(
        current: Project,
        step: Int,
    ) {
        val openProjects: List<Project> =
            service.tabs(current).filterIsInstance<WindowTab.Open>().map { tab: WindowTab.Open ->
                tab.project
            }

        if (openProjects.size < 2) {
            return
        }

        val index: Int = openProjects.indexOf(current)
        val target: Project = openProjects[Math.floorMod(index + step, openProjects.size)]
        service.switchTo(current, target)
    }
}
