package br.dev.ddr.windowtabs

import java.awt.Dimension
import java.awt.Frame
import java.awt.Rectangle

public data class FramePlacement(
    val extendedState: Int,
    val bounds: Rectangle,
) {
    public companion object {
        /**
         * A maximized source keeps the target maximized, but on the source's screen: the target first moves there at
         * its own [targetSize], or it would maximize on whatever monitor it happened to be on.
         */
        public fun matching(
            sourceState: Int,
            sourceBounds: Rectangle,
            targetSize: Dimension,
        ): FramePlacement {
            if (sourceState and Frame.MAXIMIZED_BOTH == Frame.MAXIMIZED_BOTH) {
                val width: Int = minOf(targetSize.width, sourceBounds.width)
                val height: Int = minOf(targetSize.height, sourceBounds.height)
                val left: Int = sourceBounds.x + (sourceBounds.width - width) / 2
                val top: Int = sourceBounds.y + (sourceBounds.height - height) / 2

                return FramePlacement(Frame.MAXIMIZED_BOTH, Rectangle(left, top, width, height))
            }

            return FramePlacement(Frame.NORMAL, Rectangle(sourceBounds))
        }
    }
}
