package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import javax.swing.JLabel

class TabDragTest {
    @Test
    fun `keeps the dragged tab and its left edge`() {
        val tab = JLabel()

        val drag = TabDrag(tab, 42)

        assertSame(tab, drag.tab)
        assertEquals(42, drag.left)
        assertEquals(TabDrag(tab, 42), drag)
    }
}
