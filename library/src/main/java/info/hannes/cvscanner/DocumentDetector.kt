package info.hannes.cvscanner

import android.content.Context
import android.os.Vibrator
import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import info.hannes.cvscanner.util.CVProcessor.findContours
import info.hannes.cvscanner.util.CVProcessor.getQuadrilateral
import info.hannes.cvscanner.util.CVProcessor.getScaleRatio
import info.hannes.cvscanner.util.CVProcessor.getUpScaledPoints
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size

class DocumentDetector(context: Context) : Detector<Document>() {
    private val hapticFeedback: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    override fun detect(frame: Frame): SparseArray<Document> {
        val detections = SparseArray<Document>()
        if (frame.bitmap != null) {
            val doc = detectDocument(frame)
            doc?.let {
                detections.append(frame.metadata.id, it)
            }
        }
        return detections
    }

    private fun detectDocument(frame: Frame): Document? {
        val imageSize = Size(frame.metadata.width.toDouble(), frame.metadata.height.toDouble())
        val src = Mat()
        Utils.bitmapToMat(frame.bitmap, src)
        val contours = findContours(src)
        src.release()
        if (contours.isNotEmpty()) {
            getQuadrilateral(contours, imageSize)?.let {
                it.points = getUpScaledPoints(it.points, getScaleRatio(imageSize))
                return Document(frame, it, hapticFeedback)
            }
        }
        return null
    }

}