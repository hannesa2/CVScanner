package info.hannes.cvscanner

import android.graphics.RectF
import info.hannes.cvscanner.util.CVProcessor
import info.hannes.visionpipeline.FrameGraphic
import info.hannes.visionpipeline.GraphicOverlay
import info.hannes.visionpipeline.Util

class BorderFrameGraphic(overlay: GraphicOverlay<*>?, private val isForPassport: Boolean) : FrameGraphic(overlay) {

    override fun getFrameRect(canvasWidth: Float, canvasHeight: Float): RectF {
        val rect: RectF
        val padding = 32f
        if (isForPassport) {
            val frameHeight: Float
            val frameWidth: Float
            if (Util.isPortraitMode(mOverlay.context)) {
                frameWidth = canvasWidth - 2 * padding
                frameHeight = frameWidth * CVProcessor.PASSPORT_ASPECT_RATIO
            } else {
                frameHeight = canvasHeight - 2 * padding
                frameWidth = frameHeight / CVProcessor.PASSPORT_ASPECT_RATIO
            }
            rect = RectF(padding, padding, frameWidth, frameHeight)
            val cx = canvasWidth / 2.0f
            val cy = canvasHeight / 2.0f
            val dx = cx - rect.centerX()
            val dy = cy - rect.centerY()
            rect.offset(dx, dy)
        } else {
            rect = RectF(padding, padding, canvasWidth - padding, canvasHeight - padding)
        }
        return rect
    }

}