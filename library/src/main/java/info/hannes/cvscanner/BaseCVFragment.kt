package info.hannes.cvscanner

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import info.hannes.cvscanner.CVScanner.ImageProcessorCallback
import info.hannes.cvscanner.util.CVProcessor
import info.hannes.cvscanner.util.SaveCallback
import info.hannes.cvscanner.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

abstract class BaseCVFragment : Fragment(), SaveCallback {

    protected open var isBusy = false
    protected var imageProcessorCallback: ImageProcessorCallback? = null

    // any coroutines launched inside this scope will run on the main thread unless stated otherwise
    private val uiScope = CoroutineScope(Dispatchers.Main)

    protected abstract fun onAfterViewCreated()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onAfterViewCreated()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ImageProcessorCallback) {
            imageProcessorCallback = context
        }
    }

    override fun onSaveTaskStarted() {
        isBusy = true
    }

    override fun onSaved(savedPath: String?) {
        Timber.d("saved at: %s", savedPath)
        imageProcessorCallback?.onImageProcessed(savedPath)
        isBusy = false
    }

    override fun onSaveFailed(error: Exception?) {
        imageProcessorCallback?.onImageProcessingFailed("Failed to save image", error)
        isBusy = false
    }

    @Synchronized
    protected fun saveCroppedImage(bitmap: Bitmap?, rotation: Int, quadPoints: Array<Point>) {
        onSaveTaskStarted()
        uiScope.launch {
            val path = imageSave(bitmap!!, rotation, quadPoints)
            Timber.d("file=$path")
            if (path.isNotBlank())
                onSaved(path)
            else
                onSaveFailed(Exception("could not save image"))
        }
        // ImageSaveTask(requireContext(), bitmap!!, rotation, quadPoints, this).execute()
    }

    private suspend fun imageSave(image: Bitmap, rotation: Int, points: Array<Point>): String {
        var imagePath = ""
        withContext(Dispatchers.IO) {
            val imageSize = Size(image.width.toDouble(), image.height.toDouble())
            val imageMat = Mat(imageSize, CvType.CV_8UC4)
            Utils.bitmapToMat(image, imageMat)
            image.recycle()
            val croppedImage = CVProcessor.fourPointTransform(imageMat, points)
            imageMat.release()
            var enhancedImage = CVProcessor.adjustBrightnessAndContrast(croppedImage, 1.0)
            croppedImage.release()
            enhancedImage = CVProcessor.sharpenImage(enhancedImage)
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
                val filename = "IMG_CVScanner_" + sdf.format(Date(System.currentTimeMillis()))
                imagePath = Util.saveImage(requireContext(), filename, enhancedImage, false)
                enhancedImage.release()
            } catch (e: IOException) {
                Timber.e(e, "saveImage")
            }
            try {
                imagePath.let {
                    Util.setExifRotation(requireContext(), Util.getUriFromPath(it), rotation)
                }
            } catch (e: IOException) {
                Timber.e(e, "setExifRotation $imagePath $rotation")
            }

            // Since we are updating the UI, do the operation on the Main dispatcher
            withContext(Dispatchers.Main) {
                // something
            }
        }
        return imagePath
    }

    companion object {
        init {
            System.loadLibrary("opencv_java4")
        }
    }
}