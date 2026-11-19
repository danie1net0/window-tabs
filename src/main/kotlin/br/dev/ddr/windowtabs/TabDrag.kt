package br.dev.ddr.windowtabs

import java.awt.Component

/** A tab being dragged, with the left edge it should be drawn at in strip coordinates. */
public data class TabDrag(
    public val tab: Component,
    public val left: Int,
)
