package info.hannes.cvscanner

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.google.android.gms.vision.Frame
import info.hannes.cvscanner.util.CVProcessor.Quadrilateral
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Holds the actual image data. Quad point are also scaled with respect to actual image.
 */
class Document(var image: Frame, var detectedQuad: Quadrilateral, hapticFeedback: Vibrator?) {

    init {
        hapticFeedback?.let {
            if (Build.VERSION.SDK_INT >= 26) {
                it.vibrate(VibrationEffect.createOneShot(HAPTIC_FEEDBACK_LENGTH.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(HAPTIC_FEEDBACK_LENGTH.toLong())
            }
        }
    }

    val maxArea: Int
        get() {
            val tl = detectedQuad.points[0]
            val tr = detectedQuad.points[1]
            val br = detectedQuad.points[2]
            val bl = detectedQuad.points[3]
            val widthA = sqrt((br.x - bl.x).pow(2.0) + (br.y - bl.y).pow(2.0))
            val widthB = sqrt((tr.x - tl.x).pow(2.0) + (tr.y - tl.y).pow(2.0))
            val dw = max(widthA, widthB)
            val heightA = sqrt((tr.x - br.x).pow(2.0) + (tr.y - br.y).pow(2.0))
            val heightB = sqrt((tl.x - bl.x).pow(2.0) + (tl.y - bl.y).pow(2.0))
            val dh = max(heightA, heightB)
            return java.lang.Double.valueOf(dw).toInt() * java.lang.Double.valueOf(dh).toInt()
        }

    companion object {
        private const val HAPTIC_FEEDBACK_LENGTH = 130
    }

}