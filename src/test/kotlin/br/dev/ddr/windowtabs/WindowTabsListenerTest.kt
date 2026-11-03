package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowTabsListenerTest {
    @Test
    fun `topic is named after the listener`() {
        assertEquals(WindowTabsListener::class.java, WindowTabsListener.TOPIC.listenerClass)
    }
}
