package info.hannes.cvscanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import online.devliving.mobilevisionpipeline.GraphicOverlay;
import online.devliving.mobilevisionpipeline.Util;
import online.devliving.mobilevisionpipeline.camera.CameraSource;
import online.devliving.mobilevisionpipeline.camera.CameraSourcePreview;
import timber.log.Timber;


public class DocumentScannerFragment extends BaseFragment implements View.OnTouchListener, DocumentTracker.DocumentDetectionListener {
    private final static String ARG_TORCH_COLOR = "torch_color";
    private final static String ARG_TORCH_COLOR_LIGHT = "torch_color_light";
    private final static String ARG_DOC_BORDER_COLOR = "doc_border_color";
    private final static String ARG_DOC_BODY_COLOR = "doc_body_color";

    private final Object mLock = new Object();

    private int torchTintColor = Color.GRAY, torchTintColorLight = Color.YELLOW;
    private int documentBorderColor = -1,
            documentBodyColor = -1;

    private ImageButton flashToggle;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<DocumentGraphic> mGraphicOverlay;
    private Util.FrameSizeProvider mFrameSizeProvider;

    // helper objects for detecting taps and pinches.
    private GestureDetector gestureDetector;

    private Detector<Document> IDDetector;
    private MediaActionSound sound = new MediaActionSound();

    private boolean isPassport = false;

    public static DocumentScannerFragment instantiate(boolean isPassport, Context context) {
        DocumentScannerFragment fragment = new DocumentScannerFragment();
        Bundle args = new Bundle();
        args.putBoolean(DocumentScannerActivity.EXTRA_IS_PASSPORT, isPassport);
        fragment.setArguments(args);

        return fragment;
    }

    public static DocumentScannerFragment instantiate(boolean isPassport, @ColorRes int docBorderColorRes,
                                                      @ColorRes int docBodyColorRes, @ColorRes int torchColor,
                                                      @ColorRes int torchColorLight) {
        DocumentScannerFragment fragment = new DocumentScannerFragment();
        Bundle args = new Bundle();
        args.putBoolean(DocumentScannerActivity.EXTRA_IS_PASSPORT, isPassport);
        args.putInt(ARG_DOC_BODY_COLOR, docBodyColorRes);
        args.putInt(ARG_DOC_BORDER_COLOR, docBorderColorRes);
        args.putInt(ARG_TORCH_COLOR, torchColor);
        args.putInt(ARG_TORCH_COLOR_LIGHT, torchColorLight);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DocumentScannerFragment);

        torchTintColor = array.getColor(R.styleable.DocumentScannerFragment_torchTint, torchTintColor);
        torchTintColorLight = array.getColor(R.styleable.DocumentScannerFragment_torchTintLight, torchTintColorLight);
        Timber.d("resolved torch tint colors");

        Resources.Theme theme = context.getTheme();
        TypedValue borderColor = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorPrimary, borderColor, true)) {
            Timber.d("resolved border color from theme");
            documentBorderColor = borderColor.resourceId > 0 ? getResources().getColor(borderColor.resourceId) : borderColor.data;
        }

        documentBorderColor = array.getColor(R.styleable.DocumentScannerFragment_documentBorderColor, documentBorderColor);

        TypedValue bodyColor = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorPrimaryDark, bodyColor, true)) {
            Timber.d("resolved body color from theme");
            documentBodyColor = bodyColor.resourceId > 0 ? getResources().getColor(bodyColor.resourceId) : bodyColor.data;
        }

        documentBodyColor = array.getColor(R.styleable.DocumentScannerFragment_documentBodyColor, documentBodyColor);

        array.recycle();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scanner_content, container, false);

        initializeViews(view);

        return view;
    }

    void initializeViews(View view) {
        mPreview = view.findViewById(R.id.preview);
        mGraphicOverlay = view.findViewById(R.id.graphicOverlay);
        flashToggle = view.findViewById(R.id.flash);

        gestureDetector = new GestureDetector(requireActivity(), new CaptureGestureListener());
        view.setOnTouchListener(this);
    }

    @Override
    protected void onAfterViewCreated() {
        Bundle args = getArguments();
        isPassport = args != null && args.getBoolean(DocumentScannerActivity.EXTRA_IS_PASSPORT, false);

        Resources.Theme theme = requireActivity().getTheme();
        TypedValue borderColor = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorPrimary, borderColor, true)) {
            documentBorderColor = borderColor.resourceId > 0 ? getResources().getColor(borderColor.resourceId) : borderColor.data;
        }

        TypedValue bodyColor = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorPrimaryDark, bodyColor, true)) {
            documentBodyColor = bodyColor.resourceId > 0 ? getResources().getColor(bodyColor.resourceId) : bodyColor.data;
        }

        documentBodyColor = args.getInt(ARG_DOC_BODY_COLOR, documentBodyColor);
        documentBorderColor = args.getInt(ARG_DOC_BORDER_COLOR, documentBorderColor);
        torchTintColor = args.getInt(ARG_TORCH_COLOR, torchTintColor);
        torchTintColorLight = args.getInt(ARG_TORCH_COLOR_LIGHT, torchTintColorLight);

        BorderFrameGraphic frameGraphic = new BorderFrameGraphic(mGraphicOverlay, isPassport);
        mFrameSizeProvider = frameGraphic;
        mGraphicOverlay.addFrame(frameGraphic);

        flashToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameraSource != null) {
                    if (mCameraSource.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                        mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    } else mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                    updateFlashButtonColor();
                }
            }
        });
    }

    void updateFlashButtonColor() {
        if (mCameraSource != null) {
            int tintColor = torchTintColor;

            if (mCameraSource.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                tintColor = torchTintColorLight;
            }

            DrawableCompat.setTint(flashToggle.getDrawable(), tintColor);
        }
    }

    @Override
    protected void onOpenCVConnected() {
        createCameraSource();
        startCameraSource();
    }

    @Override
    protected void onOpenCVConnectionFailed() {
        if (mCallback != null) mCallback.onImageProcessingFailed("Could not load OpenCV", null);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        if (isPassport) {
            IDDetector = new PassportDetector(mFrameSizeProvider, requireContext());
        } else
            IDDetector = new DocumentDetector(requireContext());

        /*
        DocumentTrackerFactory factory = new DocumentTrackerFactory(mGraphicOverlay, this);
        IDDetector.setProcessor(
                new MultiProcessor.Builder<>(factory).build());*/
        DocumentGraphic graphic = new DocumentGraphic(mGraphicOverlay, null);
        if (documentBorderColor != -1) graphic.setBorderColor(documentBorderColor);
        if (documentBodyColor != -1) graphic.setFillColor(documentBodyColor);

        DocumentProcessor processor = new DocumentProcessor(IDDetector,
                new DocumentTracker(mGraphicOverlay, graphic, this));
        IDDetector.setProcessor(processor);

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        mCameraSource = new CameraSource.Builder(requireActivity().getApplicationContext(), IDDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                .setFlashMode(Camera.Parameters.FLASH_MODE_AUTO)
                .setRequestedFps(15.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }

        if (sound != null) sound.release();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (Exception e) {
                Timber.e(e, "Unable to start camera source.");
                Toast.makeText(requireActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    void processDocument(Document document) {
        synchronized (mLock) {
            saveCroppedImage(document.getImage().getBitmap(), document.getImage().getMetadata().getRotation(),
                    document.detectedQuad.points);
            isBusy = true;
        }
    }

    @Override
    public void onDocumentDetected(final Document document) {
        Timber.d("document detected");
        if (document != null) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCameraSource != null)
                        mCameraSource.stop();
                    processDocument(document);
                }
            });
        }
    }

    private void detectDocumentManually(final byte[] data) {
        Timber.d("detecting document manually");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (image != null) {
                    final SparseArray<Document> docs = IDDetector.detect(new Frame.Builder()
                            .setBitmap(image)
                            .build());

                    if (docs != null && docs.size() > 0) {
                        Timber.d("detected document manually");
                        final Document doc = docs.get(0);

                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processDocument(doc);
                            }
                        });
                    } else {
                        requireActivity().finish();
                    }
                }
            }
        }).start();
    }

    void takePicture() {
        if (mCameraSource != null) {
            mCameraSource.takePicture(new CameraSource.ShutterCallback() {
                @Override
                public void onShutter() {
                    sound.play(MediaActionSound.SHUTTER_CLICK);
                }
            }, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data) {
                    detectDocumentManually(data);
                }
            });
        }
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Timber.d("fragment got touch");
        boolean g = gestureDetector.onTouchEvent(event);

        return g || v.onTouchEvent(event);
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        boolean hasShownMsg = false;

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Timber.d("fragment got tap");
            if (!hasShownMsg) {
                Toast.makeText(requireActivity(), "Double tap to take a picture and force detection", Toast.LENGTH_SHORT).show();
                hasShownMsg = true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            takePicture();
            return true;
        }
    }
}
