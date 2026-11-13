package br.dev.ddr.windowtabs

import java.awt.Frame
import java.awt.Rectangle

public data class FramePlacement(
    val extendedState: Int,
    val bounds: Rectangle?,
) {
    public companion object {
        public fun matching(
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
