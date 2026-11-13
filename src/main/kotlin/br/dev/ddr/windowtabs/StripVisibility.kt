package br.dev.ddr.windowtabs

public object StripVisibility {
    public fun shows(
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
