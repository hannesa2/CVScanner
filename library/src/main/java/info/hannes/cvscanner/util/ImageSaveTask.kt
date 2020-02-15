package info.hannes.cvscanner.util

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import info.hannes.cvscanner.util.CVProcessor.adjustBrightnessAndContrast
import info.hannes.cvscanner.util.CVProcessor.fourPointTransform
import info.hannes.cvscanner.util.CVProcessor.sharpenImage
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImageSaveTask(private val mContext: Context, private val image: Bitmap, private val rotation: Int, private val points: Array<Point>, private val mCallback: SaveCallback) : AsyncTask<Void?, Void?, String?>() {
    override fun onPreExecute() {
        mCallback.onSaveTaskStarted()
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to [.execute]
     * by the caller of this task.
     *
     *
     * This method can call [.publishProgress] to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see .onPreExecute
     * @see .onPostExecute
     *
     * @see .publishProgress
     */
    override fun doInBackground(vararg params: Void?): String? {
        val imageSize = Size(image.width.toDouble(), image.height.toDouble())
        val imageMat = Mat(imageSize, CvType.CV_8UC4)
        Utils.bitmapToMat(image, imageMat)
        image.recycle()
        val croppedImage = fourPointTransform(imageMat, points)
        imageMat.release()
        var enhancedImage = adjustBrightnessAndContrast(croppedImage, 1.0)
        croppedImage.release()
        enhancedImage = sharpenImage(enhancedImage)
        var imagePath: String? = null
        try {
            val sdf = SimpleDateFormat("YYYY-MM-dd_HHmmss", Locale.getDefault())
            val filename = "IMG_CVScanner_" + sdf.format(Date(System.currentTimeMillis()))
            imagePath = Util.saveImage(mContext, filename, enhancedImage, false)
            enhancedImage.release()
        } catch (e: IOException) {
            Timber.e(e, "saveImage")
        }
        try {
            imagePath?.let {
                Util.setExifRotation(mContext, Util.getUriFromPath(it), rotation)
            }
        } catch (e: IOException) {
            Timber.e(e, "setExifRotation $imagePath $rotation")
        }
        return imagePath
    }

    override fun onPostExecute(path: String?) {
        if (path != null) mCallback.onSaved(path) else mCallback.onSaveFailed(Exception("could not save image"))
    }

    interface SaveCallback {
        fun onSaveTaskStarted()
        fun onSaved(savedPath: String?)
        fun onSaveFailed(error: Exception?)
    }

}