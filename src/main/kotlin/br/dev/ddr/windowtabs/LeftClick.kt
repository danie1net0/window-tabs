package br.dev.ddr.windowtabs

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * Runs [action] when the left button is let go over the component it was pressed on. AWT's own click event is no use
 * here: X11 drops it once the mouse moves a few pixels between press and release.
 * Only the button let go counts, not the ones still held: a chord must not count as a left click.
 */
public class LeftClick(
    private val action: (MouseEvent) -> Unit,
) : MouseAdapter() {
    override fun mouseReleased(event: MouseEvent) {
        if (event.button != MouseEvent.BUTTON1 || !event.component.contains(event.point)) {
            return
        }

        action(event)
    }
}
