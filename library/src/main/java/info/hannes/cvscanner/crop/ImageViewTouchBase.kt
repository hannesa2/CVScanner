/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.hannes.cvscanner.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatImageView

abstract class ImageViewTouchBase : AppCompatImageView {

    val bitmapDisplayed = RotateBitmap(null)

    // This is the final matrix which is computed as the concatenation
    // of the base matrix and the supplementary matrix.
    private val mDisplayMatrix = Matrix()

    // Temporary buffer used for getting the values out of a matrix.
    private val mMatrixValues = FloatArray(9)

    // This is the base transformation which is used to show the image
    // initially.  The current computation for this shows the image in
    // it's entirety, letterboxing as needed.  One could choose to
    // show the image as cropped instead.
    //
    // This matrix is recomputed when we go from the thumbnail image to
    // the full size image.
    protected var mBaseMatrix = Matrix()

    // This is the supplementary transformation which reflects what
    // the user has done in terms of zooming and panning.
    //
    // This matrix remains the same when we go from the thumbnail image
    // to the full size image.
    protected var mSuppMatrix = Matrix()
    protected var mHandler = Handler(Looper.getMainLooper())
    var mThisWidth = -1
    private var mThisHeight = -1

    @JvmField
    var mMaxZoom = 0f

    @JvmField
    var mLeft = 0

    @JvmField
    var mRight = 0

    @JvmField
    var mTop = 0

    @JvmField
    var mBottom = 0
    private var mOnLayoutRunnable: Runnable? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        init()
    }

    override fun onLayout(
        changed: Boolean, left: Int, top: Int,
        right: Int, bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        mLeft = left
        mRight = right
        mTop = top
        mBottom = bottom
        mThisWidth = right - left
        mThisHeight = bottom - top
        val r = mOnLayoutRunnable
        if (r != null) {
            mOnLayoutRunnable = null
            r.run()
        }
        if (bitmapDisplayed.bitmap != null) {
            getProperBaseMatrix(bitmapDisplayed, mBaseMatrix)
            imageMatrix = imageViewMatrix
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && scale > 1.0f) {
            // If we're zoomed in, pressing Back jumps out to show the entire
            // image, otherwise Back returns the user to the gallery.
            zoomTo(1.0f)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun setImageBitmap(bitmap: Bitmap?) {
        setImageBitmap(bitmap, 0)
    }

    private fun setImageBitmap(bitmap: Bitmap?, rotation: Int) {
        val old = bitmapDisplayed.bitmap
        if (bitmap == null) {
            super@ImageViewTouchBase.setImageBitmap(null)
            clear()
            bitmapDisplayed.bitmap = null
            return
        }
        if (drawable != null) {
            val oldTransition = drawable as TransitionDrawable
            oldTransition.resetTransition()
            if (oldTransition.numberOfLayers == 2) {
                val layer0 = oldTransition.getDrawable(0) as BitmapDrawable
                layer0.bitmap.recycle()
            }
            if (old?.height != bitmap.height || old.width != bitmap.width || bitmapDisplayed.rotation != rotation) {
                if (oldTransition.numberOfLayers == 2) {
                    val layer1 = oldTransition.getDrawable(1) as BitmapDrawable
                    layer1.bitmap.recycle()
                }
                val layers = arrayOf(BitmapDrawable(resources, bitmap))
                val transitionDrawable = TransitionDrawable(layers)
                setImageDrawable(transitionDrawable)
            } else {
                val layers = arrayOf(
                    BitmapDrawable(resources, old), BitmapDrawable(
                        resources, bitmap
                    )
                )
                val transitionDrawable = TransitionDrawable(layers)
                setImageDrawable(transitionDrawable)
                transitionDrawable.startTransition(TRANSITION_DURATION)
            }
        } else {
            val layers = arrayOf(BitmapDrawable(resources, bitmap))
            val transitionDrawable = TransitionDrawable(layers)
            setImageDrawable(transitionDrawable)
        }
        bitmapDisplayed.bitmap = bitmap
        bitmapDisplayed.rotation = rotation
    }

    fun clear() {
        val drawable = drawable
        if (drawable is BitmapDrawable) {
            val bd = getDrawable() as BitmapDrawable
            if (bd.bitmap != null) {
                bd.bitmap.recycle()
            }
        } else if (drawable is TransitionDrawable) {
            for (i in 0 until drawable.numberOfLayers) {
                val layer = drawable.getDrawable(i) as BitmapDrawable
                if (layer.bitmap != null) {
                    layer.bitmap.recycle()
                }
            }
        }
    }

    // This function changes bitmap, reset base matrix according to the size
    // of the bitmap, and optionally reset the supplementary matrix.
    fun setImageBitmapResetBase(bitmap: Bitmap?, resetSupp: Boolean, rotation: Int) {
        setImageRotateBitmapResetBase(RotateBitmap(bitmap!!, rotation), resetSupp)
    }

    fun setImageRotateBitmapResetBase(bitmap: RotateBitmap, resetSupp: Boolean) {
        val viewWidth = width
        if (viewWidth <= 0) {
            mOnLayoutRunnable = Runnable { setImageRotateBitmapResetBase(bitmap, resetSupp) }
            return
        }
        if (bitmap.bitmap != null) {
            getProperBaseMatrix(bitmap, mBaseMatrix)
            setImageBitmap(bitmap.bitmap, bitmap.rotation)
        } else {
            mBaseMatrix.reset()
            setImageBitmap(null)
        }
        if (resetSupp) {
            mSuppMatrix.reset()
        }
        imageMatrix = imageViewMatrix
        mMaxZoom = maxZoom()
    }

    // Center as much as possible in one or both axis.  Centering is
    // defined as follows:  if the image is scaled down below the
    // view's dimensions then center it (literally).  If the image
    // is scaled larger than the view and is translated out of view
    // then translate it back into view (i.e. eliminate black bars).
    protected fun center(horizontal: Boolean, vertical: Boolean) {
        if (bitmapDisplayed.bitmap == null) {
            return
        }
        val m = imageViewMatrix
        val rect = RectF(
            0.0.toFloat(), 0.0.toFloat(),
            bitmapDisplayed.bitmap?.width?.toFloat() ?: 0.0.toFloat(),
            bitmapDisplayed.bitmap?.height?.toFloat() ?: 0.0.toFloat()
        )
        m.mapRect(rect)
        val height = rect.height()
        val width = rect.width()
        var deltaX = 0f
        var deltaY = 0f
        if (vertical) {
            val viewHeight = getHeight()
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top
            } else if (rect.top > 0) {
                deltaY = -rect.top
            } else if (rect.bottom < viewHeight) {
                deltaY = getHeight() - rect.bottom
            }
        }
        if (horizontal) {
            val viewWidth = getWidth()
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left
            } else if (rect.left > 0) {
                deltaX = -rect.left
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right
            }
        }
        postTranslate(deltaX, deltaY)
        imageMatrix = imageViewMatrix
    }

    private fun init() {
        scaleType = ScaleType.MATRIX
    }

    protected fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[whichValue]
    }

    // Get the scale factor out of the matrix.
    protected fun getScale(matrix: Matrix): Float {
        return getValue(matrix, Matrix.MSCALE_X)
    }

    protected val scale: Float
        get() = getScale(mSuppMatrix)

    // Setup the base matrix so that the image is centered and scaled properly.
    private fun getProperBaseMatrix(bitmap: RotateBitmap, matrix: Matrix) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        //int rotation = bitmap.getRotation();
        matrix.reset()

        // We limit up-scaling to 2x otherwise the result may look bad if it's
        // a small icon.
        val widthScale = Math.min(viewWidth / w, 2.0f)
        val heightScale = Math.min(viewHeight / h, 2.0f)
        val scale = Math.min(widthScale, heightScale)
        matrix.postConcat(bitmap.rotationMatrix)
        matrix.postScale(scale, scale)
        matrix.postTranslate(
            (viewWidth - w * scale) / 2f,
            (viewHeight - h * scale) / 2f
        )
    }// The final matrix is computed as the concatenation of the base matrix

    // and the supplementary matrix.
    // Combine the base matrix and the supp matrix to make the final matrix.
    protected val imageViewMatrix: Matrix
        get() {
            // The final matrix is computed as the concatenation of the base matrix
            // and the supplementary matrix.
            mDisplayMatrix.set(mBaseMatrix)
            mDisplayMatrix.postConcat(mSuppMatrix)
            return mDisplayMatrix
        }

    // Sets the maximum zoom, which is a scale relative to the base matrix. It
    // is calculated to show the image at 400% zoom regardless of screen or
    // image orientation. If in the future we decode the full 3 megapixel image,
    // rather than the current 1024x768, this should be changed down to 200%.
    protected fun maxZoom(): Float {
        if (bitmapDisplayed.bitmap == null) {
            return 1f
        }
        val fw = bitmapDisplayed.width.toFloat() / mThisWidth.toFloat()
        val fh = bitmapDisplayed.height.toFloat() / mThisHeight.toFloat()
        return Math.max(fw, fh) * 2
    }

    protected open fun zoomTo(scaleValue: Float, centerX: Float, centerY: Float) {
        var scale = scaleValue
        if (scale > mMaxZoom) {
            scale = mMaxZoom
        }
        val oldScale = scale
        val deltaScale = scale / oldScale
        mSuppMatrix.postScale(deltaScale, deltaScale, centerX, centerY)
        imageMatrix = imageViewMatrix
        center(true, true)
    }

    protected fun zoomTo(
        scale: Float, centerX: Float,
        centerY: Float, durationMs: Float
    ) {
        val incrementPerMs = (scale - scale) / durationMs
        val startTime = System.currentTimeMillis()
        mHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val currentMs = Math.min(durationMs, (now - startTime).toFloat())
                val target = scale + incrementPerMs * currentMs
                zoomTo(target, centerX, centerY)
                if (currentMs < durationMs) {
                    mHandler.post(this)
                } else {
                    onZoomFinished()
                }
            }
        })
    }

    abstract fun onZoomFinished()
    private fun zoomTo(scale: Float) {
        val cx = width / 2f
        val cy = height / 2f
        zoomTo(scale, cx, cy)
    }

    protected fun zoomTo(scale: Float, durationMs: Float) {
        val cx = width / 2f
        val cy = height / 2f
        zoomTo(scale, cx, cy, durationMs)
    }

    protected open fun zoomIn() {
        zoomIn(SCALE_RATE)
    }

    protected open fun zoomOut() {
        zoomOut(SCALE_RATE)
    }

    protected fun zoomIn(rate: Float) {
        if (scale >= mMaxZoom) {
            return  // Don't let the user zoom into the molecular level.
        }
        if (bitmapDisplayed.bitmap == null) {
            return
        }
        val cx = width / 2f
        val cy = height / 2f
        mSuppMatrix.postScale(rate, rate, cx, cy)
        imageMatrix = imageViewMatrix
    }

    protected fun zoomOut(rate: Float) {
        if (bitmapDisplayed.bitmap == null) {
            return
        }
        val cx = width / 2f
        val cy = height / 2f

        // Zoom out to at most 1x.
        val tmp = Matrix(mSuppMatrix)
        tmp.postScale(1f / rate, 1f / rate, cx, cy)
        if (getScale(tmp) < 1f) {
            mSuppMatrix.setScale(1f, 1f, cx, cy)
        } else {
            mSuppMatrix.postScale(1f / rate, 1f / rate, cx, cy)
        }
        imageMatrix = imageViewMatrix
        center(true, true)
    }

    protected open fun postTranslate(dx: Float, dy: Float) {
        mSuppMatrix.postTranslate(dx, dy)
    }

    protected fun panBy(dx: Float, dy: Float) {
        postTranslate(dx, dy)
        imageMatrix = imageViewMatrix
    }

    companion object {
        const val TRANSITION_DURATION = 500
        const val SCALE_RATE = 1.25f
    }
}