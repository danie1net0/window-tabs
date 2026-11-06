package br.dev.ddr.windowtabs

/** Whether a frame shows the strip, given how many tabs it would have. */
object StripVisibility {
    fun shows(
        tabCount: Int,
        options: WindowTabsSettings.Options,
        isMac: Boolean,
    ): Boolean {
        if (isMac && !options.showOnMac) {
            return false
        }

        var minimum = 2
        if (options.showWithSingleProject) {
            minimum = 1
        }

        return tabCount >= minimum
    }
}
