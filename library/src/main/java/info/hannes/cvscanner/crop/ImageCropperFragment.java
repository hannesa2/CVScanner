package info.hannes.cvscanner.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Point;

import java.io.IOException;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import info.hannes.cvscanner.BaseCVFragment;
import info.hannes.cvscanner.R;
import info.hannes.cvscanner.util.CVProcessor;
import info.hannes.cvscanner.util.Util;


public class ImageCropperFragment extends BaseCVFragment implements CropImageView.CropImageViewHost {
    final static String ARG_SRC_IMAGE_URI = "source_image";
    final static String ARG_RT_LEFT_IMAGE_RES = "rotateLeft_imageRes";
    final static String ARG_SAVE_IMAGE_RES = "save_imageRes";
    final static String ARG_RT_RIGHT_IMAGE_RES = "rotateRight_imageRes";
    final static String ARG_SAVE_IMAGE_COLOR_RES = "save_imageColorRes";
    final static String ARG_RT_IMAGE_COLOR_RES = "rotate_imageColorRes";
    final static String ARG_RT_ICONS_VISIBILITY = "rotate_icons_visibility";
    protected CropImageView mImageView;
    protected ImageButton mRotateLeft;
    protected ImageButton mRotateRight;
    protected ImageButton mSave;
    protected CropHighlightView mCrop;
    protected int mRotation = 0, mScaleFactor = 1;
    protected Bitmap mBitmap;
    protected Uri imageUri = null;
    protected ImageLoadingCallback mImageLoadingCallback = null;
    protected Boolean rtIconsVisibility;

    public static ImageCropperFragment instantiate(Uri imageUri) {
        ImageCropperFragment fragment = new ImageCropperFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SRC_IMAGE_URI, imageUri.toString());
        fragment.setArguments(args);

        return fragment;
    }

    public static ImageCropperFragment instantiate(Uri imageUri, @ColorRes int buttonTint,
                                                   @ColorRes int buttonTintSecondary, @DrawableRes int rotateLeftRes,
                                                   @DrawableRes int rotateRightRes, @DrawableRes int saveButtonRes, Boolean rtIconsVisibility) {
        ImageCropperFragment fragment = new ImageCropperFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SRC_IMAGE_URI, imageUri.toString());
        args.putInt(ARG_SAVE_IMAGE_COLOR_RES, buttonTint);
        args.putInt(ARG_RT_IMAGE_COLOR_RES, buttonTintSecondary);
        args.putInt(ARG_RT_LEFT_IMAGE_RES, rotateLeftRes);
        args.putInt(ARG_RT_RIGHT_IMAGE_RES, rotateRightRes);
        args.putInt(ARG_SAVE_IMAGE_RES, saveButtonRes);
        args.putBoolean(ARG_RT_ICONS_VISIBILITY, rtIconsVisibility);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);

        if (context instanceof ImageLoadingCallback) {
            mImageLoadingCallback = (ImageLoadingCallback) context;
        }
        startCropping();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.imagecropper_content, container, false);

        initializeViews(view);

        return view;
    }

    private void initializeViews(View view) {
        mImageView = view.findViewById(R.id.cropImageView);
        mRotateLeft = view.findViewById(R.id.item_rotate_left);
        mRotateRight = view.findViewById(R.id.item_rotate_right);
        mSave = view.findViewById(R.id.item_save);

        mImageView.setHost(this);

        Bundle extras = getArguments();

        if (extras.containsKey(ARG_SRC_IMAGE_URI)) {
            imageUri = Uri.parse(extras.getString(ARG_SRC_IMAGE_URI));
        }

        int buttonTintColor = getResources().getColor(extras.getInt(ARG_SAVE_IMAGE_COLOR_RES, R.color.colorAccent));
        int secondaryBtnTintColor = getResources().getColor(extras.getInt(ARG_RT_IMAGE_COLOR_RES, R.color.colorPrimary));
        Drawable saveBtnDrawable = getResources().getDrawable(extras.getInt(ARG_SAVE_IMAGE_RES, R.drawable.ic_check_circle));
        Drawable rotateLeftDrawable = getResources().getDrawable(extras.getInt(ARG_RT_LEFT_IMAGE_RES, R.drawable.ic_rotate_left));
        Drawable rotateRightDrawable = getResources().getDrawable(extras.getInt(ARG_RT_RIGHT_IMAGE_RES, R.drawable.ic_rotate_right));
        rtIconsVisibility = extras.getBoolean(ARG_RT_ICONS_VISIBILITY);

        DrawableCompat.setTint(rotateLeftDrawable, secondaryBtnTintColor);
        mRotateLeft.setImageDrawable(rotateLeftDrawable);

        DrawableCompat.setTint(rotateRightDrawable, secondaryBtnTintColor);
        mRotateRight.setImageDrawable(rotateRightDrawable);

        DrawableCompat.setTint(saveBtnDrawable, buttonTintColor);
        mSave.setImageDrawable(saveBtnDrawable);
    }

    @Override
    protected void onAfterViewCreated() {
        mRotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotateRight();
            }
        });

        mRotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotateLeft();
            }
        });

        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cropAndSave();
            }
        });
    }

    public void rotateLeft() {
        rotate(-1);
    }

    public void rotateRight() {
        rotate(1);
    }

    private void rotate(int delta) {
        if (mBitmap != null) {
            if (delta < 0) {
                delta = -delta * 3;
            }
            mRotation += delta;
            mRotation = mRotation % 4;
            mImageView.setImageBitmapResetBase(mBitmap, false, mRotation * 90);
            showDefaultCroppingRectangle(mBitmap.getWidth(), mBitmap.getHeight());
        }
    }

    private void startCropping() {
        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                Exception error = null;
                if (imageUri != null) {
                    try {
                        mScaleFactor = Util.calculateBitmapSampleSize(requireContext(), imageUri);
                        mBitmap = Util.INSTANCE.loadBitmapFromUri(requireContext(), mScaleFactor, imageUri);

                        int rotation = Util.getExifRotation(requireContext(), imageUri);
                        mRotation = rotation / 90;
                    } catch (IOException e) {
                        error = e;
                    }
                }

                if (mBitmap != null) {
                    mImageView.setImageBitmapResetBase(mBitmap, true, mRotation * 90);
                    adjustButtons();
                    showDefaultCroppingRectangle(mBitmap.getWidth(), mBitmap.getHeight());

                    if (mImageLoadingCallback != null) mImageLoadingCallback.onImageLoaded();
                } else {
                    if (mImageLoadingCallback != null)
                        mImageLoadingCallback.onFailedToLoadImage(error != null ?
                                error : new IllegalArgumentException("failed to load bitmap from provided uri"));
                    else
                        throw (error != null ? new IllegalStateException(error) : new IllegalArgumentException("failed to load bitmap from provided uri"));
                }

                mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }

        });
    }

    private void adjustButtons() {
        if (mBitmap != null) {
            if (!rtIconsVisibility) {
                mRotateLeft.setVisibility(View.VISIBLE);
                mRotateRight.setVisibility(View.VISIBLE);
            }
            mSave.setVisibility(View.VISIBLE);
        } else {
            mRotateLeft.setVisibility(View.GONE);
            mRotateRight.setVisibility(View.GONE);
            mSave.setVisibility(View.GONE);
        }
    }

    private void showDefaultCroppingRectangle(int imageWidth, int imageHeight) {
        Rect imageRect = new Rect(0, 0, imageWidth, imageHeight);

        // make the default size about 4/5 of the width or height
        int cropWidth = Math.min(imageWidth, imageHeight) * 4 / 5;


        int x = (imageWidth - cropWidth) / 2;
        int y = (imageHeight - cropWidth) / 2;

        RectF cropRect = new RectF(x, y, x + cropWidth, y + cropWidth);

        CropHighlightView hv = new CropHighlightView(mImageView, imageRect, cropRect);

        mImageView.resetMaxZoom();
        mImageView.add(hv);
        mCrop = hv;
        mCrop.setFocus(true);
        mImageView.invalidate();
    }

    public void cropAndSave() {
        if (mBitmap != null && !isBusy()) {
            float[] points = mCrop.getTrapezoid();
            Point[] quadPoints = new Point[4];

            for (int i = 0, j = 0; i < 8; i++, j++) {
                quadPoints[j] = new Point(points[i], points[++i]);
            }

            Point[] sortedPoints = CVProcessor.INSTANCE.sortPoints(quadPoints);
            saveCroppedImage(mBitmap, mRotation, sortedPoints);
        }
    }

    protected void clearImages() {
        if (mBitmap != null && !mBitmap.isRecycled()) mBitmap.recycle();
        mImageView.clear();
    }

    @Override
    public void onSaved(String path) {
        super.onSaved(path);
        clearImages();
    }

    @Override
    public void onSaveFailed(Exception error) {
        super.onSaveFailed(error);
        clearImages();
    }

    @Override
    public boolean isBusy() {
        return super.isBusy();
    }

    public interface ImageLoadingCallback {
        void onImageLoaded();

        void onFailedToLoadImage(Exception error);
    }
}
