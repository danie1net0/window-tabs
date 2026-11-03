package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Test

class TabOrderTest {
    @Test
    fun `keeps the order projects were opened in`() {
        val order = TabOrder()

        assertEquals(listOf("a", "b"), order.sync(listOf("a", "b")))
        assertEquals(listOf("a", "b", "c"), order.sync(listOf("c", "b", "a")))
    }

    @Test
    fun `drops closed projects and appends new ones at the end`() {
        val order = TabOrder()
        order.sync(listOf("a", "b", "c"))

        assertEquals(listOf("a", "c", "d"), order.sync(listOf("d", "c", "a")))
    }

    @Test
    fun `is empty when nothing is open`() {
        val order = TabOrder()
        order.sync(listOf("a"))

        assertEquals(emptyList<String>(), order.sync(emptyList()))
    }
}
