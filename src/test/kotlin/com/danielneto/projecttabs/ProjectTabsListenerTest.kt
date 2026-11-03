package com.danielneto.projecttabs

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectTabsListenerTest {
    @Test
    fun `topic is named after the listener`() {
        assertEquals(ProjectTabsListener::class.java, ProjectTabsListener.TOPIC.listenerClass)
    }
}
