package info.hannes.cvscanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Camera
import android.media.MediaActionSound
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import info.hannes.cvscanner.DocumentTracker.DocumentDetectionListener
import info.hannes.cvscanner.databinding.ScannerContentBinding
import info.hannes.visionpipeline.GraphicOverlay
import info.hannes.visionpipeline.Util.FrameSizeProvider
import info.hannes.visionpipeline.camera.CameraSource
import timber.log.Timber

class DocumentScannerFragment : BaseCVFragment(), View.OnTouchListener, DocumentDetectionListener {

    private val mLock = Any()
    private var torchTintColor = Color.GRAY
    private var torchTintColorLight = Color.YELLOW
    private var documentBorderColor = -1
    private var documentBodyColor = -1
    private var cameraSource: CameraSource? = null
    private var frameSizeProvider: FrameSizeProvider? = null

    // helper objects for detecting taps and pinches.
    private var gestureDetector: GestureDetector? = null
    private var detectorID: Detector<Document>? = null
    private val sound: MediaActionSound = MediaActionSound()
    private var isPassport = false

    private var _binding: ScannerContentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)
        val array = context.obtainStyledAttributes(attrs, R.styleable.DocumentScannerFragment)
        try {
            torchTintColor =
                array.getColor(R.styleable.DocumentScannerFragment_torchTint, torchTintColor)
            torchTintColorLight = array.getColor(
                R.styleable.DocumentScannerFragment_torchTintLight,
                torchTintColorLight
            )
            Timber.d("resolved torch tint colors")
            val theme = context.theme
            val borderColor = TypedValue()
            if (theme.resolveAttribute(android.R.attr.colorPrimary, borderColor, true)) {
                Timber.d("resolved border color from theme")
                documentBorderColor = if (borderColor.resourceId > 0) ContextCompat.getColor(
                    context,
                    borderColor.resourceId
                ) else borderColor.data
            }
            documentBorderColor = array.getColor(
                R.styleable.DocumentScannerFragment_documentBorderColor,
                documentBorderColor
            )
            val bodyColor = TypedValue()
            if (theme.resolveAttribute(android.R.attr.colorPrimaryDark, bodyColor, true)) {
                Timber.d("resolved body color from theme")
                documentBodyColor = if (bodyColor.resourceId > 0) ContextCompat.getColor(
                    context,
                    bodyColor.resourceId
                ) else bodyColor.data
            }
            documentBodyColor = array.getColor(
                R.styleable.DocumentScannerFragment_documentBodyColor,
                documentBodyColor
            )
        } finally {
            array.recycle()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ScannerContentBinding.inflate(inflater, container, false)
        val view = binding.root
        gestureDetector = GestureDetector(requireActivity(), CaptureGestureListener())
        view.setOnTouchListener(this)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sound.release()
        _binding = null
    }

    override fun onAfterViewCreated() {
        isPassport = requireArguments().getBoolean(DocumentScannerActivity.EXTRA_IS_PASSPORT, false)
        val theme = requireActivity().theme
        val borderColor = TypedValue()
        if (theme.resolveAttribute(android.R.attr.colorPrimary, borderColor, true)) {
            documentBorderColor = if (borderColor.resourceId > 0) ContextCompat.getColor(
                requireContext(),
                borderColor.resourceId
            ) else borderColor.data
        }
        val bodyColor = TypedValue()
        if (theme.resolveAttribute(android.R.attr.colorPrimaryDark, bodyColor, true)) {
            documentBodyColor = if (bodyColor.resourceId > 0) ContextCompat.getColor(
                requireContext(),
                bodyColor.resourceId
            ) else bodyColor.data
        }
        documentBodyColor = requireArguments().getInt(ARG_DOC_BODY_COLOR, documentBodyColor)
        documentBorderColor = requireArguments().getInt(ARG_DOC_BORDER_COLOR, documentBorderColor)
        torchTintColor = requireArguments().getInt(ARG_TORCH_COLOR, torchTintColor)
        torchTintColorLight = requireArguments().getInt(ARG_TORCH_COLOR_LIGHT, torchTintColorLight)
        val frameGraphic = BorderFrameGraphic(binding.graphicOverlay, isPassport)
        frameSizeProvider = frameGraphic
        binding.graphicOverlay.addFrame(frameGraphic)
        binding.flashToggle.setOnClickListener {
            cameraSource?.let {
                if (it.flashMode == Camera.Parameters.FLASH_MODE_TORCH) {
                    it.setFlashMode(Camera.Parameters.FLASH_MODE_OFF)
                } else
                    it.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                updateFlashButtonColor()
            }
        }

        createCameraSource()
        startCameraSource()
    }

    private fun updateFlashButtonColor() {
        cameraSource?.let {
            var tintColor = torchTintColor
            if (it.flashMode == Camera.Parameters.FLASH_MODE_TORCH) {
                tintColor = torchTintColorLight
            }
            DrawableCompat.setTint(binding.flashToggle.drawable, tintColor)
        }
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private fun createCameraSource() {
        detectorID = if (isPassport) {
            PassportDetector(frameSizeProvider!!, requireContext())
        } else
            DocumentDetector(requireContext())

        val graphic = DocumentGraphic(binding.graphicOverlay, null)
        if (documentBorderColor != -1) graphic.setBorderColor(documentBorderColor)
        if (documentBodyColor != -1) graphic.setFillColor(documentBodyColor)
        val processor = DocumentProcessor(
            detectorID!!,
            DocumentTracker(binding.graphicOverlay as GraphicOverlay<DocumentGraphic>, graphic, this)
        )
        detectorID!!.setProcessor(processor)

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes at long distances.
        cameraSource = CameraSource.Builder(requireActivity().applicationContext, detectorID)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
            .setFlashMode(Camera.Parameters.FLASH_MODE_AUTO)
            .setRequestedFps(15.0f)
            .build()
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        binding.cameraSourcePreview.stop()
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @Throws(SecurityException::class)
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                binding.cameraSourcePreview.start(cameraSource, binding.graphicOverlay)
            } catch (e: Exception) {
                Timber.e(e, "Unable to start camera source.")
                Toast.makeText(requireActivity(), e.message, Toast.LENGTH_LONG).show()
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    private fun processDocument(document: Document) {
        synchronized(mLock) {
            isBusy = true
            saveCroppedImage(
                document.image.bitmap,
                document.image.metadata.rotation,
                document.detectedQuad.points
            )
            isBusy = false
        }
    }

    override fun onDocumentDetected(document: Document) {
        Timber.d("document detected")
        requireActivity().runOnUiThread {
            if (cameraSource != null)
                cameraSource!!.stop()
            processDocument(document)
        }
    }

    private fun detectDocumentManually(data: ByteArray) {
        Timber.d("detecting document manually")
        Thread {
            BitmapFactory.decodeByteArray(data, 0, data.size)?.let {
                val docs = detectorID!!.detect(
                    Frame.Builder()
                        .setBitmap(it)
                        .build()
                )
                if (docs.size() > 0) {
                    Timber.d("detected document manually")
                    val doc = docs[0]
                    requireActivity().runOnUiThread { processDocument(doc) }
                } else {
                    Timber.d("detected finish")
                    requireActivity().finish()
                }
            }
        }.start()
    }

    fun takePicture() {
        if (cameraSource != null) {
            cameraSource!!.takePicture({ sound.play(MediaActionSound.SHUTTER_CLICK) }) { data ->
                detectDocumentManually(
                    data
                )
            }
        }
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     * the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        Timber.d("fragment got touch")
        val g = gestureDetector!!.onTouchEvent(event)
        return g || v.onTouchEvent(event)
    }

    private inner class CaptureGestureListener : GestureDetector.SimpleOnGestureListener() {
        var hasShownMsg = false
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            Timber.d("fragment got tap")
            if (!hasShownMsg) {
                Toast.makeText(
                    requireActivity(),
                    "Double tap to take a picture and force detection",
                    Toast.LENGTH_SHORT
                ).show()
                hasShownMsg = true
            }
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            takePicture()
            return true
        }
    }

    companion object {
        private const val ARG_TORCH_COLOR = "torch_color"
        private const val ARG_TORCH_COLOR_LIGHT = "torch_color_light"
        private const val ARG_DOC_BORDER_COLOR = "doc_border_color"
        private const val ARG_DOC_BODY_COLOR = "doc_body_color"
        fun instantiate(isPassport: Boolean): DocumentScannerFragment {
            val fragment = DocumentScannerFragment()
            val args = Bundle()
            args.putBoolean(DocumentScannerActivity.EXTRA_IS_PASSPORT, isPassport)
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun instantiate(
            isPassport: Boolean, @ColorRes docBorderColorRes: Int,
            @ColorRes docBodyColorRes: Int, @ColorRes torchColor: Int,
            @ColorRes torchColorLight: Int
        ): DocumentScannerFragment {
            val fragment = DocumentScannerFragment()
            val args = Bundle().apply {
                putBoolean(DocumentScannerActivity.EXTRA_IS_PASSPORT, isPassport)
                putInt(ARG_DOC_BODY_COLOR, docBodyColorRes)
                putInt(ARG_DOC_BORDER_COLOR, docBorderColorRes)
                putInt(ARG_TORCH_COLOR, torchColor)
                putInt(ARG_TORCH_COLOR_LIGHT, torchColorLight)
            }

            fragment.arguments = args
            return fragment
        }
    }
}
