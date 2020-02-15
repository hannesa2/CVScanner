package info.hannes.cvscanner

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import info.hannes.cvscanner.CVScanner.ImageProcessorCallback
import info.hannes.cvscanner.util.ImageSaveTask
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Point
import timber.log.Timber

abstract class BaseFragment : Fragment(), ImageSaveTask.SaveCallback {

    protected open var isBusy = false
    protected var imageProcessorCallback: ImageProcessorCallback? = null
    private var loaderCallback: BaseLoaderCallback? = null

    private fun loadOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, requireActivity().applicationContext, loaderCallback)
        } else {
            loaderCallback!!.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    protected abstract fun onOpenCVConnected()
    protected abstract fun onOpenCVConnectionFailed()
    protected abstract fun onAfterViewCreated()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onAfterViewCreated()
        loadOpenCV()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        loaderCallback = object : BaseLoaderCallback(context) {
            override fun onManagerConnected(status: Int) {
                if (status == LoaderCallbackInterface.SUCCESS) {
                    onOpenCVConnected()
                } else {
                    onOpenCVConnectionFailed()
                }
            }
        }
        if (context is ImageProcessorCallback) {
            imageProcessorCallback = context
        }
    }

    override fun onSaveTaskStarted() {
        isBusy = true
    }

    override fun onSaved(savedPath: String?) {
        Timber.d("saved at: %s", savedPath)
        if (imageProcessorCallback != null) imageProcessorCallback!!.onImageProcessed(savedPath)
        isBusy = false
    }

    override fun onSaveFailed(error: Exception?) {
        if (imageProcessorCallback != null) imageProcessorCallback!!.onImageProcessingFailed("Failed to save image", error)
        isBusy = false
    }

    @Synchronized
    protected fun saveCroppedImage(bitmap: Bitmap?, rotation: Int, quadPoints: Array<Point>) {
        if (!isBusy) {
            ImageSaveTask(requireContext(), bitmap!!, rotation, quadPoints, this).execute()
        }
    }
}