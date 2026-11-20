package br.dev.ddr.windowtabs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.event.MouseEvent
import javax.swing.JLabel

class LeftClickTest {
    private val label = JLabel().apply { setSize(20, 20) }
    private val clicks = mutableListOf<MouseEvent>()

    private fun release(
        button: Int,
        horizontalPosition: Int,
    ) {
        label.dispatchEvent(MouseEvent(label, MouseEvent.MOUSE_RELEASED, 0, 0, horizontalPosition, 5, 1, false, button))
    }

    @Test
    fun `acts on a left release over the component`() {
        label.addMouseListener(LeftClick { event: MouseEvent -> clicks.add(event) })

        release(MouseEvent.BUTTON1, 5)

        assertEquals(1, clicks.size)
    }

    @Test
    fun `a chord counts only the button let go`() {
        label.addMouseListener(LeftClick { event: MouseEvent -> clicks.add(event) })
        val left: Int = MouseEvent.BUTTON1_DOWN_MASK

        label.dispatchEvent(MouseEvent(label, MouseEvent.MOUSE_RELEASED, 0, left, 5, 5, 1, false, MouseEvent.BUTTON3))

        assertEquals(0, clicks.size)
    }

    @Test
    fun `ignores other buttons and releases outside`() {
        label.addMouseListener(LeftClick { event: MouseEvent -> clicks.add(event) })

        release(MouseEvent.BUTTON3, 5)
        release(MouseEvent.BUTTON2, 5)
        release(MouseEvent.BUTTON1, 40)

        assertEquals(0, clicks.size)
    }
}
