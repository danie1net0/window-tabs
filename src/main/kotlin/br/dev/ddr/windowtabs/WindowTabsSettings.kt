package br.dev.ddr.windowtabs

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/** What the user chose in Settings | Appearance & Behavior | Window Tabs, plus the order they dragged the tabs into. */
@Service(Service.Level.APP)
@State(name = "WindowTabs", storages = [Storage("windowTabs.xml")])
class WindowTabsSettings : SimplePersistentStateComponent<WindowTabsSettings.Options>(Options()) {
    class Options : BaseState() {
        /** Shows the strip even when only one project is open. */
        var showWithSingleProject by property(false)

        /** Puts each project's icon, the one the Welcome screen shows, in front of its name. */
        var showProjectIcons by property(true)

        /** macOS already merges project windows into native tabs, so the strip stays off there unless asked for. */
        var showOnMac by property(false)

        /** Tab keys (project paths) in the order the user left them; restores the order on the next start. */
        var tabOrder by list<String>()
    }

    companion object {
        fun getInstance(): WindowTabsSettings = service()
    }
}
