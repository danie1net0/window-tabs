package br.dev.ddr.windowtabs

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "WindowTabs", storages = [Storage("windowTabs.xml")])
public class WindowTabsSettings : SimplePersistentStateComponent<WindowTabsSettings.Options>(Options()) {
    public class Options : BaseState() {
        public var showWithSingleProject: Boolean by property(false)

        public var showProjectIcons: Boolean by property(true)

        /** macOS already merges project windows into native tabs, so the strip stays off there unless asked for. */
        public var showOnMac: Boolean by property(false)

        public var tabOrder: MutableList<String> by list<String>()
    }

    public companion object {
        public fun getInstance(): WindowTabsSettings = service()
    }
}
