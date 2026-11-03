package br.dev.ddr.windowtabs

import com.intellij.util.messages.Topic

fun interface WindowTabsListener {
    fun tabsChanged()

    companion object {
        @JvmField
        @Topic.AppLevel
        val TOPIC = Topic(WindowTabsListener::class.java, Topic.BroadcastDirection.NONE)
    }
}
