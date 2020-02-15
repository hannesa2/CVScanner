package info.hannes.cvscanner

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.os.Vibrator
import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import info.hannes.cvscanner.util.CVProcessor.Quadrilateral
import info.hannes.cvscanner.util.CVProcessor.findContoursForMRZ
import info.hannes.cvscanner.util.CVProcessor.getQuadForPassport
import info.hannes.cvscanner.util.CVProcessor.getScaleRatio
import info.hannes.cvscanner.util.CVProcessor.getUpScaledPoints
import online.devliving.mobilevisionpipeline.Util.FrameSizeProvider
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PassportDetector(sizeProvider: FrameSizeProvider, context: Context) : Detector<Document>() {
    private val mHapticFeedback: Vibrator
    private val frameSizeProvider: FrameSizeProvider?
    override fun detect(frame: Frame): SparseArray<Document> {
        val detections = SparseArray<Document>()
        val doc = detectDocument(frame)
        if (doc != null) detections.append(frame.metadata.id, doc)
        return detections
    }

    fun saveBitmapJPG(img: Bitmap, imageName: String?): String? {
        val dir = File(Environment.getExternalStorageDirectory(), "/" + "CVScanner" + "/")
        dir.mkdirs()
        val file = File(dir, imageName)
        val fOut: FileOutputStream
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            fOut = FileOutputStream(file)
            img.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
            fOut.flush()
            fOut.close()
            return file.absolutePath
        } catch (e: IOException) {
            Timber.e(e)
        }
        return null
    }

    private fun detectDocument(frame: Frame): Document? {
        var imageSize = Size(frame.metadata.width.toDouble(), frame.metadata.height.toDouble())
        var src = Mat()
        Utils.bitmapToMat(frame.bitmap, src)
        val shiftX: Int
        val shiftY: Int
        val frameWidth = frameSizeProvider!!.frameWidth()
        val frameHeight = frameSizeProvider.frameHeight()
        shiftX = java.lang.Double.valueOf((imageSize.width - frameHeight) / 2.0).toInt()
        shiftY = java.lang.Double.valueOf((imageSize.height - frameWidth) / 2.0).toInt()
        val rect = Rect(shiftX, shiftY, frameHeight, frameWidth)
        val cropped = Mat(src, rect).clone()
        src.release()
        src = cropped
        imageSize = Size(src.cols().toDouble(), src.rows().toDouble())
        var quad: Quadrilateral? = null
        val isMRZBasedDetection = false
        if (isMRZBasedDetection) {
            val contours = findContoursForMRZ(src)
            if (!contours.isEmpty()) {
                quad = getQuadForPassport(contours, imageSize, frameSizeProvider.frameWidth())
            }
        } else {
            quad = getQuadForPassport(src, frameSizeProvider.frameWidth().toDouble(),
                    frameSizeProvider.frameHeight().toDouble())
        }
        src.release()
        if (quad != null) {
            quad.points = getUpScaledPoints(quad.points, getScaleRatio(imageSize))
            //shift back to old coordinates
            for (i in 0 until quad.points.size) {
                quad.points[i] = shiftPointToOld(quad.points[i], shiftX, shiftY)
            }
            return Document(frame, quad, mHapticFeedback)
        }
        return null
    }

    private fun shiftPointToOld(point: Point, sx: Int, sy: Int): Point {
        point.x = point.x + sx
        point.y = point.y + sy
        return point
    }

    init {
        frameSizeProvider = sizeProvider
        mHapticFeedback = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}