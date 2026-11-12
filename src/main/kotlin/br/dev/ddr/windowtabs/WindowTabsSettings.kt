package br.dev.ddr.windowtabs

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "WindowTabs", storages = [Storage("windowTabs.xml")])
class WindowTabsSettings : SimplePersistentStateComponent<WindowTabsSettings.Options>(Options()) {
    class Options : BaseState() {
        var showWithSingleProject by property(false)

        var showProjectIcons by property(true)

        /** macOS already merges project windows into native tabs, so the strip stays off there unless asked for. */
        var showOnMac by property(false)

        var tabOrder by list<String>()
    }

    companion object {
        fun getInstance(): WindowTabsSettings = service()
    }
}
