package com.danielneto.projecttabs

import com.intellij.util.messages.Topic

fun interface ProjectTabsListener {
    fun tabsChanged()

    companion object {
        @JvmField
        @Topic.AppLevel
        val TOPIC = Topic(ProjectTabsListener::class.java, Topic.BroadcastDirection.NONE)
    }
}
