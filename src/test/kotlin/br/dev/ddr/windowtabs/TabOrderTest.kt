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

    @Test
    fun `starts from the saved order`() {
        val order = TabOrder(listOf("b", "a", "b"))

        assertEquals(listOf("b", "a", "c"), order.sync(listOf("a", "c", "b")))
    }

    @Test
    fun `moves a tab and reports the new order`() {
        val reported = mutableListOf<List<String>>()
        val order = TabOrder { keys: List<String> -> reported.add(keys) }
        order.sync(listOf("a", "b", "c"))

        order.move("c", 0)
        order.move("a", 99)

        assertEquals(listOf("c", "b", "a"), order.sync(listOf("a", "b", "c")))
        assertEquals(listOf(listOf("a", "b", "c"), listOf("c", "a", "b"), listOf("c", "b", "a")), reported)
    }

    @Test
    fun `reports nothing when the order stays the same`() {
        val reported = mutableListOf<List<String>>()
        val order = TabOrder(listOf("a", "b")) { keys: List<String> -> reported.add(keys) }

        order.sync(listOf("a", "b"))
        order.move("a", 0)
        order.move("missing", 1)

        assertEquals(emptyList<List<String>>(), reported)
    }
}
