package info.hannes.cvscanner

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.shapes.PathShape
import info.hannes.visionpipeline.GraphicOverlay
import info.hannes.visionpipeline.GraphicOverlay.Graphic
import timber.log.Timber

class DocumentGraphic(overlay: GraphicOverlay<*>?, private var scannedDoc: Document?) : Graphic(overlay) {
    var id = 0
    private val borderPaint: Paint
    private val bodyPaint: Paint
    private var borderColor = Color.parseColor("#41fa97")
    private var fillColor = Color.parseColor("#69fbad")

    fun update(doc: Document?) {
        scannedDoc = doc
        postInvalidate()
    }

    fun setBorderColor(borderColor: Int) {
        this.borderColor = borderColor
        borderPaint.color = borderColor
    }

    fun setFillColor(fillColor: Int) {
        this.fillColor = fillColor
        bodyPaint.color = fillColor
    }

    /**
     * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
     * convert to view coordinates for the graphics that are drawn:
     *
     *  1. [GraphicOverlay.Graphic.scaleX] and [GraphicOverlay.Graphic.scaleY] adjust the size of
     * the supplied value from the preview scale to the view scale.
     *  1. [GraphicOverlay.Graphic.translateX] and [GraphicOverlay.Graphic.translateY] adjust the
     * coordinate from the preview's coordinate system to the view coordinate system.
     *
     *
     * @param canvas drawing canvas
     */
    override fun draw(canvas: Canvas) { //TODO fix the coordinates see http://zhengrui.github.io/android-coordinates.html
        if (scannedDoc != null) { //boolean isPortrait = Util.isPortraitMode(mOverlay.getContext());
            val path = Path()
            /*
            Timber.d("IsPortrait? " + isPortrait);

            float tlX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[0].y):translateX((float) scannedDoc.detectedQuad.points[0].x);
            float tlY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[0].x):translateY((float) scannedDoc.detectedQuad.points[0].y);

            Timber.d("Top left: x: " + scannedDoc.detectedQuad.points[0].x + ", y: " + scannedDoc.detectedQuad.points[0].y
                    + " -> x: " + tlX + ", y: " + tlY);

            float blX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[1].y):translateX((float) scannedDoc.detectedQuad.points[1].x);
            float blY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[1].x):translateY((float) scannedDoc.detectedQuad.points[1].y);

            Timber.d("Bottom left: x: " + scannedDoc.detectedQuad.points[1].x + ", y: " + scannedDoc.detectedQuad.points[1].y
                    + " -> x: " + blX + ", y: " + blY);

            float brX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[2].y):translateX((float) scannedDoc.detectedQuad.points[2].x);
            float brY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[2].x):translateY((float) scannedDoc.detectedQuad.points[2].y);

            Timber.d("Bottom right: x: " + scannedDoc.detectedQuad.points[2].x + ", y: " + scannedDoc.detectedQuad.points[2].y
                    + " -> x: " + brX + ", y: " + brY);

            float trX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[3].y):translateX((float) scannedDoc.detectedQuad.points[3].x);
            float trY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[3].x):translateY((float) scannedDoc.detectedQuad.points[3].y);

            Timber.d("Top right: x: " + scannedDoc.detectedQuad.points[3].x + ", y: " + scannedDoc.detectedQuad.points[3].y
                    + " -> x: " + trX + ", y: " + trY);
            */
            val frameWidth = scannedDoc!!.image.metadata.height
            path.moveTo((frameWidth - scannedDoc!!.detectedQuad.points[0].y).toFloat(), scannedDoc!!.detectedQuad.points[0].x.toFloat())
            path.lineTo((frameWidth - scannedDoc!!.detectedQuad.points[1].y).toFloat(), scannedDoc!!.detectedQuad.points[1].x.toFloat())
            path.lineTo((frameWidth - scannedDoc!!.detectedQuad.points[2].y).toFloat(), scannedDoc!!.detectedQuad.points[2].x.toFloat())
            path.lineTo((frameWidth - scannedDoc!!.detectedQuad.points[3].y).toFloat(), scannedDoc!!.detectedQuad.points[3].x.toFloat())
            path.close()
            val shape = PathShape(path, scannedDoc!!.image.metadata.height.toFloat(), scannedDoc!!.image.metadata.width.toFloat())
            shape.resize(canvas.width.toFloat(), canvas.height.toFloat())
            shape.draw(canvas, bodyPaint)
            shape.draw(canvas, borderPaint)
            //canvas.drawPath(path, borderPaint);
            //canvas.drawPath(path, bodyPaint);
            Timber.d("DONE DRAWING")
        }
    }

    init {
        borderPaint = Paint()
        borderPaint.color = borderColor
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeCap = Paint.Cap.ROUND
        borderPaint.strokeJoin = Paint.Join.ROUND
        borderPaint.strokeWidth = 12f
        bodyPaint = Paint()
        bodyPaint.color = fillColor
        bodyPaint.alpha = 180
        bodyPaint.style = Paint.Style.FILL
    }
}