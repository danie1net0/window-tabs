package br.dev.ddr.windowtabs

import com.intellij.util.messages.Topic

public fun interface WindowTabsListener {
    public fun tabsChanged()

    public companion object {
        @JvmField
        @Topic.AppLevel
        public val TOPIC: Topic<WindowTabsListener> =
            Topic(WindowTabsListener::class.java, Topic.BroadcastDirection.NONE)
    }
}
