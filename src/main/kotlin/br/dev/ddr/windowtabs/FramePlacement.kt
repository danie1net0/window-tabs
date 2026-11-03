package br.dev.ddr.windowtabs

import java.awt.Frame
import java.awt.Rectangle

/**
 * Where the frame of the tab being opened must go so it covers the frame being left:
 * same maximized state, or the same bounds when the source is a normal window.
 */
data class FramePlacement(
    val extendedState: Int,
    val bounds: Rectangle?,
) {
    companion object {
        fun matching(
            sourceState: Int,
            sourceBounds: Rectangle,
        ): FramePlacement {
            if (sourceState and Frame.MAXIMIZED_BOTH == Frame.MAXIMIZED_BOTH) {
                return FramePlacement(Frame.MAXIMIZED_BOTH, null)
            }

            return FramePlacement(Frame.NORMAL, Rectangle(sourceBounds))
        }
    }
}
