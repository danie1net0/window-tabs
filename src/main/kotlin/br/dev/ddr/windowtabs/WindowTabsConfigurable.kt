package br.dev.ddr.windowtabs

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class WindowTabsConfigurable : BoundConfigurable("Window Tabs") {
    override fun createPanel(): DialogPanel {
        val options = WindowTabsSettings.getInstance().state

        return panel {
            group("Tab Strip") {
                row {
                    checkBox("Show the strip with a single project open")
                        .bindSelected(options::showWithSingleProject)
                }
                row {
                    checkBox("Show project icons on the tabs")
                        .bindSelected(options::showProjectIcons)
                }
                row {
                    checkBox("Show the strip on macOS")
                        .bindSelected(options::showOnMac)
                        .comment(MAC_COMMENT)
                }
            }
            group("Shortcuts") {
                row {
                    comment(SHORTCUTS_COMMENT)
                }
            }
        }
    }

    override fun apply() {
        super.apply()

        WindowTabsService.getInstance().notifyChanged()
    }

    private companion object {
        const val MAC_COMMENT =
            "macOS can already merge project windows into native tabs (Window | Merge All Project Windows)."
        const val SHORTCUTS_COMMENT =
            "Alt+Shift+1…9 go to a tab. Next Project Tab and Previous Project Tab have no shortcut by default; " +
                "assign them in Settings | Keymap."
    }
}
