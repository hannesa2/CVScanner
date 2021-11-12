/*
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package info.hannes.cvscanner.crop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import info.hannes.cvscanner.crop.HighLightView.Companion.GROW_NONE

class CropImageView(context: Context?, attrs: AttributeSet?) : ImageViewTouchBase(context, attrs) {

    private var cropHighlightView: HighLightView? = null
    private var mIsMoving = false
    private var mLastX = 0f
    private var mLastY = 0f
    private var mMotionEdge = 0
    private var mHost: CropImageViewHost? = null

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        cropHighlightView?.let {
            it.matrix!!.set(imageMatrix)
            centerBasedOnHighlightView(it)
        }
    }

    override fun zoomTo(scale: Float, centerX: Float, centerY: Float) {
        super.zoomTo(scale, centerX, centerY)
        cropHighlightView?.let {
            it.matrix!!.set(imageMatrix)
        }
    }

    override fun zoomIn() {
        super.zoomIn()
        cropHighlightView?.let {
            it.matrix!!.set(imageMatrix)
        }
    }

    override fun zoomOut() {
        super.zoomOut()
        if (cropHighlightView != null) {
            cropHighlightView!!.matrix!!.set(imageMatrix)
        }
    }

    override fun postTranslate(dx: Float, dy: Float) {
        super.postTranslate(dx, dy)
        cropHighlightView?.let {
            it.matrix!!.postTranslate(dx, dy)
        }
    }

    fun setHost(mHost: CropImageViewHost?) {
        this.mHost = mHost
    }

    private fun mapPointToImageSpace(x: Float, y: Float): FloatArray {
        val p = FloatArray(2)
        val m = imageViewMatrix
        val m2 = Matrix()
        m.invert(m2)
        p[0] = x
        p[1] = y
        m2.mapPoints(p)
        return p
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mHost!!.isBusy) {
            return false
        }
        if (cropHighlightView == null) {
            return false
        }
        val mappedPoint = mapPointToImageSpace(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val edge = cropHighlightView!!.getHit(mappedPoint[0], mappedPoint[1], scale)
                if (edge != GROW_NONE) {
                    mMotionEdge = edge
                    mIsMoving = true
                    mLastX = mappedPoint[0]
                    mLastY = mappedPoint[1]
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mIsMoving) {
                    centerBasedOnHighlightView(cropHighlightView!!)
                }
                mIsMoving = false
                center(true, true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mIsMoving) {
                    cropHighlightView!!.handleMotion(
                        mMotionEdge,
                        mappedPoint[0] - mLastX,
                        mappedPoint[1] - mLastY
                    )
                    mLastX = mappedPoint[0]
                    mLastY = mappedPoint[1]
                    ensureVisible(cropHighlightView!!)
                    invalidate()
                }
                // if we're not zoomed then there's no point in even allowing
                // the user to move the image around. This call to center puts
                // it back to the normalized location (with false meaning don't
                // animate).
                if (scale == 1f) {
                    center(true, true)
                }
            }
        }
        return true
    }

    override fun onZoomFinished() {
        if (cropHighlightView != null) {
            ensureVisible(cropHighlightView!!)
        }
    }

    // Pan the displayed image to make sure the cropping rectangle is visible.
    private fun ensureVisible(hv: HighLightView) {
        val r = hv.drawRect
        val panDeltaX1 = Math.max(0, mLeft - r!!.left)
        val panDeltaX2 = Math.min(0, mRight - r.right)
        val panDeltaY1 = Math.max(0, mTop - r.top)
        val panDeltaY2 = Math.min(0, mBottom - r.bottom)
        val panDeltaX = if (panDeltaX1 != 0) panDeltaX1 else panDeltaX2
        val panDeltaY = if (panDeltaY1 != 0) panDeltaY1 else panDeltaY2
        if (panDeltaX != 0 || panDeltaY != 0) {
            panBy(panDeltaX.toFloat(), panDeltaY.toFloat())
        }
    }

    // If the cropping rectangle's size changed significantly, change the
    // view's center and scale according to the cropping rectangle.
    private fun centerBasedOnHighlightView(hv: HighLightView) {
        val drawRect = hv.drawRect
        val width = drawRect!!.width().toFloat()
        val height = drawRect.height().toFloat()
        val thisWidth = getWidth().toFloat()
        val thisHeight = getHeight().toFloat()
        val z1 = thisWidth / width * .6f
        val z2 = thisHeight / height * .6f
        var zoom = Math.min(z1, z2)
        zoom = zoom * scale
        zoom = Math.max(1f, zoom)
        if (Math.abs(zoom - scale) / zoom > .1) {
            val coordinates = floatArrayOf(hv.centerX(), hv.centerY())
            imageMatrix.mapPoints(coordinates)
            zoomTo(zoom, coordinates[0], coordinates[1], 300f)
        }
        ensureVisible(hv)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isInEditMode && cropHighlightView != null) {
            cropHighlightView!!.draw(canvas)
        }
    }

    fun add(hv: HighLightView?) {
        cropHighlightView = hv
        invalidate()
    }

    fun setMaxZoom(maxZoom: Int) {
        mMaxZoom = maxZoom.toFloat()
    }

    fun resetMaxZoom() {
        mMaxZoom = maxZoom()
    }

    interface CropImageViewHost {
        val isBusy: Boolean
    }

    init {
        if (context is CropImageViewHost) mHost = context
    }
}