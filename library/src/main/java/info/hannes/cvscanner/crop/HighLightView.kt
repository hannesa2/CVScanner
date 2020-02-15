package info.hannes.cvscanner.crop

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect

interface HighLightView {
    /**
     * @return Matrix that converts between image and screen space.
     */
    val matrix: Matrix?

    /**
     * @return Drawing rect in screen space.
     */
    val drawRect: Rect?

    /**
     * @return vertical center in image space.
     */
    fun centerY(): Float

    /**
     * @return horizontal center in image space.
     */
    fun centerX(): Float

    fun getHit(x: Float, y: Float, scale: Float): Int
    fun handleMotion(motionEdge: Int, dx: Float, dy: Float)
    fun draw(canvas: Canvas?)

    companion object {
        const val GROW_NONE = 0
        const val GROW_LEFT_EDGE = 1 shl 1
        const val GROW_RIGHT_EDGE = 1 shl 2
        const val GROW_TOP_EDGE = 1 shl 3
        const val GROW_BOTTOM_EDGE = 1 shl 4
        const val MOVE = 1 shl 5
    }
}