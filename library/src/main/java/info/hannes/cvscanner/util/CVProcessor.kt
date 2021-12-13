package info.hannes.cvscanner.util

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import java.util.*
import kotlin.math.*

object CVProcessor {

    const val PASSPORT_ASPECT_RATIO = 3.465f / 4.921f
    private const val FIXED_HEIGHT = 800

    fun buildMatFromYUV(nv21Data: ByteArray?, width: Int, height: Int): Mat {
        val yuv = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuv.put(0, 0, nv21Data)
        val rgba = Mat()
        Imgproc.cvtColor(yuv, rgba, Imgproc.COLOR_YUV2RGBA_NV21, CvType.CV_8UC4)
        return rgba
    }

    fun detectBorder(original: Mat): Rect {
        val src = original.clone()
        Timber.d("1 original: $src")
        Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 0.0)
        Timber.d("2.1 --> Gaussian blur done\n blur: $src")
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY)
        Timber.d("2.2 --> Grayscaling done\n gray: $src")
        val sobelX = Mat()
        val sobelY = Mat()
        Imgproc.Sobel(src, sobelX, CvType.CV_32FC1, 2, 0, 5, 1.0, 0.0)
        Timber.d("3.1 --> Sobel done.\n X: $sobelX")
        Imgproc.Sobel(src, sobelY, CvType.CV_32FC1, 0, 2, 5, 1.0, 0.0)
        Timber.d("3.2 --> Sobel done.\n Y: $sobelY")
        val sum_img = Mat()
        Core.addWeighted(sobelX, 0.5, sobelY, 0.5, 0.5, sum_img)
        //Core.add(sobelX, sobelY, sum_img);
        Timber.d("4 --> Addition done. sum: $sum_img")
        sobelX.release()
        sobelY.release()
        val gray = Mat()
        Core.normalize(sum_img, gray, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1)
        Timber.d("5 --> Normalization done. gray: $gray")
        sum_img.release()
        val row_proj = Mat()
        val col_proj = Mat()
        Core.reduce(gray, row_proj, 1, Core.REDUCE_AVG, CvType.CV_8UC1)
        Timber.d("6.1 --> Reduce done. row: $row_proj")
        Core.reduce(gray, col_proj, 0, Core.REDUCE_AVG, CvType.CV_8UC1)
        Timber.d("6.2 --> Reduce done. col: $col_proj")
        gray.release()
        Imgproc.Sobel(row_proj, row_proj, CvType.CV_8UC1, 0, 2)
        Timber.d("7.1 --> Sobel done. row: $row_proj")
        Imgproc.Sobel(col_proj, col_proj, CvType.CV_8UC1, 2, 0)
        Timber.d("7.2 --> Sobel done. col: $col_proj")
        val result = Rect()
        var half_pos = (row_proj.total() / 2).toInt()
        val row_sub = Mat(row_proj, Range(0, half_pos), Range(0, 1))
        Timber.d("8.1 --> Copy sub matrix done. row: $row_sub")
        result.y = Core.minMaxLoc(row_sub).maxLoc.y.toInt()
        Timber.d("8.2 --> Minmax done. Y: ${result.y}")
        row_sub.release()
        val row_sub2 = Mat(row_proj, Range(half_pos, row_proj.total().toInt()), Range(0, 1))
        Timber.d("8.3 --> Copy sub matrix done. row: $row_sub2")
        result.height = (Core.minMaxLoc(row_sub2).maxLoc.y + half_pos - result.y).toInt()
        Timber.d("8.4 --> Minmax done. Height: ${result.height}")
        row_sub2.release()
        half_pos = (col_proj.total() / 2).toInt()
        val col_sub = Mat(col_proj, Range(0, 1), Range(0, half_pos))
        Timber.d("9.1 --> Copy sub matrix done. col: $col_sub")
        result.x = Core.minMaxLoc(col_sub).maxLoc.x.toInt()
        Timber.d("9.2 --> Minmax done. X: ${result.x}")
        col_sub.release()
        val col_sub2 = Mat(col_proj, Range(0, 1), Range(half_pos, col_proj.total().toInt()))
        Timber.d("9.3 --> Copy sub matrix done. col: $col_sub2")
        result.width = (Core.minMaxLoc(col_sub2).maxLoc.x + half_pos - result.x).toInt()
        Timber.d("9.4 --> Minmax done. Width: ${result.width}")
        col_sub2.release()
        row_proj.release()
        col_proj.release()
        src.release()
        return result
    }

    fun getScaleRatio(srcSize: Size): Double {
        return srcSize.height / FIXED_HEIGHT
    }

    fun findContours(src: Mat): List<MatOfPoint> {
        val img = src.clone()
        //find contours
        val ratio = getScaleRatio(img.size())
        val width = (img.size().width / ratio).toInt()
        val height = (img.size().height / ratio).toInt()
        val newSize = Size(width.toDouble(), height.toDouble())
        val resizedImg = Mat(newSize, CvType.CV_8UC4)
        Imgproc.resize(img, resizedImg, newSize)
        img.release()
        Imgproc.medianBlur(resizedImg, resizedImg, 7)
        val cannedImg = Mat(newSize, CvType.CV_8UC1)
        Imgproc.Canny(resizedImg, cannedImg, 70.0, 200.0, 3, true)
        resizedImg.release()
        Imgproc.threshold(cannedImg, cannedImg, 70.0, 255.0, Imgproc.THRESH_OTSU)
        val dilatedImg = Mat(newSize, CvType.CV_8UC1)
        val morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.dilate(cannedImg, dilatedImg, morph, Point((-1).toDouble(), (-1).toDouble()), 2, 1, Scalar(1.0))
        cannedImg.release()
        morph.release()
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(dilatedImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()
        dilatedImg.release()
        Timber.d("contours found: ${contours.size}")
        contours.sortWith(Comparator { o1, o2 -> java.lang.Double.compare(Imgproc.contourArea(o2), Imgproc.contourArea(o1)) })
        return contours
    }

    fun findContoursForMRZ(src: Mat): List<MatOfPoint> {
        val img = src.clone()
        src.release()
        val ratio = getScaleRatio(img.size())
        val width = (img.size().width / ratio).toInt()
        val height = (img.size().height / ratio).toInt()
        val newSize = Size(width.toDouble(), height.toDouble())
        val resizedImg = Mat(newSize, CvType.CV_8UC4)
        Imgproc.resize(img, resizedImg, newSize)
        val gray = Mat()
        Imgproc.cvtColor(resizedImg, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.medianBlur(gray, gray, 3)
        //Imgproc.blur(gray, gray, new Size(3, 3));
        var morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(13.0, 5.0))
        val dilatedImg = Mat()
        Imgproc.morphologyEx(gray, dilatedImg, Imgproc.MORPH_BLACKHAT, morph)
        gray.release()
        val gradX = Mat()
        Imgproc.Sobel(dilatedImg, gradX, CvType.CV_32F, 1, 0)
        dilatedImg.release()
        Core.convertScaleAbs(gradX, gradX, 1.0, 0.0)
        val minMax = Core.minMaxLoc(gradX)
        Core.convertScaleAbs(
            gradX, gradX, 255 / (minMax.maxVal - minMax.minVal),
            -(minMax.minVal * 255 / (minMax.maxVal - minMax.minVal))
        )
        Imgproc.morphologyEx(gradX, gradX, Imgproc.MORPH_CLOSE, morph)
        val thresh = Mat()
        Imgproc.threshold(gradX, thresh, 0.0, 255.0, Imgproc.THRESH_OTSU)
        gradX.release()
        morph.release()
        morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(21.0, 21.0))
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, morph)
        Imgproc.erode(thresh, thresh, Mat(), Point((-1).toDouble(), (-1).toDouble()), 4)
        morph.release()
        val col = resizedImg.size().width.toInt()
        val p = (resizedImg.size().width * 0.05).toInt()
        val row = resizedImg.size().height.toInt()
        for (i in 0 until row) {
            for (j in 0 until p) {
                thresh.put(i, j, 0.0)
                thresh.put(i, col - j, 0.0)
            }
        }
        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()
        Timber.d("contours found: ${contours.size}")
        Collections.sort(contours) { o1, o2 -> java.lang.Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1)) }
        return contours
    }

    fun findContoursAfterClosing(src: Mat): List<MatOfPoint?> {
        val img = src.clone()
        //find contours
        val ratio = getScaleRatio(img.size())
        val width = (img.size().width / ratio).toInt()
        val height = (img.size().height / ratio).toInt()
        val newSize = Size(width.toDouble(), height.toDouble())
        val resizedImg = Mat(newSize, CvType.CV_8UC4)
        Imgproc.resize(img, resizedImg, newSize)
        img.release()
        Imgproc.medianBlur(resizedImg, resizedImg, 5)
        val cannedImg = Mat(newSize, CvType.CV_8UC1)
        Imgproc.Canny(resizedImg, cannedImg, 70.0, 200.0, 3, true)
        resizedImg.release()
        Imgproc.threshold(cannedImg, cannedImg, 70.0, 255.0, Imgproc.THRESH_OTSU)
        val dilatedImg = Mat(newSize, CvType.CV_8UC1)
        val morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.dilate(cannedImg, dilatedImg, morph, Point((-1).toDouble(), (-1).toDouble()), 2, 1, Scalar(1.0))
        cannedImg.release()
        morph.release()
        var contours = ArrayList<MatOfPoint?>()
        var hierarchy = Mat()
        Imgproc.findContours(dilatedImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()
        Timber.d("contours found: ${contours.size}")
        contours.sortWith(Comparator { o1, o2 -> java.lang.Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1)) })
        val box = Imgproc.boundingRect(contours[0])
        Imgproc.line(dilatedImg, box.tl(), Point(box.br().x, box.tl().y), Scalar(255.0, 255.0, 255.0), 2)
        contours = ArrayList()
        hierarchy = Mat()
        Imgproc.findContours(dilatedImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()
        dilatedImg.release()
        Timber.d("contours found: ${contours.size}")
        contours.sortWith(Comparator { o1, o2 -> java.lang.Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1)) })
        return contours
    }

    fun getQuadForPassport(img: Mat, frameWidthIn: Double, frameHeightIn: Double): Quadrilateral? {
        var frameWidth = frameWidthIn
        var frameHeight = frameHeightIn
        val requiredCoverageRatio = 0.60
        val ratio = getScaleRatio(img.size())
        val width = img.size().width / ratio
        val height = img.size().height / ratio
        if (frameHeight == 0.0 || frameWidth == 0.0) {
            frameWidth = width
            frameHeight = height
        } else {
            frameWidth = frameWidth / ratio
            frameHeight = frameHeight / ratio
        }
        val newSize = Size(width, height)
        val resizedImg = Mat(newSize, CvType.CV_8UC4)
        Imgproc.resize(img, resizedImg, newSize)
        Imgproc.medianBlur(resizedImg, resizedImg, 13)
        val cannedImg = Mat(newSize, CvType.CV_8UC1)
        Imgproc.Canny(resizedImg, cannedImg, 70.0, 200.0, 3, true)
        resizedImg.release()
        val morphR = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, Size(5.0, 5.0))
        Imgproc.morphologyEx(cannedImg, cannedImg, Imgproc.MORPH_CLOSE, morphR, Point((-1).toDouble(), (-1).toDouble()), 1)
        val lines = MatOfFloat4()
        Imgproc.HoughLinesP(cannedImg, lines, 1.0, Math.PI / 180, 30, 30.0, 150.0)
        if (lines.rows() >= 3) {
            val hLines = ArrayList<Line>()
            val vLines = ArrayList<Line>()
            for (i in 0 until lines.rows()) {
                val vec = lines[i, 0]
                val l = Line(vec[0], vec[1], vec[2], vec[3])
                if (l.isNearHorizontal) hLines.add(l) else if (l.isNearVertical) vLines.add(l)
            }
            if (hLines.size >= 2 && vLines.size >= 2) {
                hLines.sortWith(Comparator { o1, o2 -> ceil(o1.start.y - o2.start.y).toInt() })
                vLines.sortWith(Comparator { o1, o2 -> ceil(o1.start.x - o2.start.x).toInt() })
                val nhLines = Line.joinSegments(hLines)
                val nvLines = Line.joinSegments(vLines)
                if (nvLines.size > 1 && nhLines.size > 0 || nvLines.size > 0 && nhLines.size > 1) {
                    nhLines.sortedWith(Comparator { o1, o2 -> ceil(o2.length() - o1.length()).toInt() })
                    nvLines.sortedWith(Comparator { o1, o2 -> Math.ceil(o2.length() - o1.length()).toInt() })
                    var left: Line? = null
                    var right: Line? = null
                    var bottom: Line? = null
                    var top: Line? = null
                    for (l in nvLines) {
                        if (l.length() / frameHeight < requiredCoverageRatio || left != null && right != null) break
                        if (left == null && l.isInleft(width)) {
                            left = l
                            continue
                        }
                        if (right == null && !l.isInleft(width)) right = l
                    }
                    for (l in nhLines) {
                        if (l.length() / frameWidth < requiredCoverageRatio || top != null && bottom != null) break
                        if (bottom == null && l.isInBottom(height)) {
                            bottom = l
                            continue
                        }
                        if (top == null && !l.isInBottom(height)) top = l
                    }
                    var foundPoints: Array<Point>? = null
                    if (left != null && right != null && (bottom != null || top != null)) {
                        val vLeft = if (bottom != null) bottom.intersect(left) else top!!.intersect(left)
                        val vRight = if (bottom != null) bottom.intersect(right) else top!!.intersect(right)
                        Timber.d("got the edges")
                        if (vLeft != null && vRight != null) {
                            val pwidth = Line(vLeft, vRight).length()
                            val pHeight = pwidth / PASSPORT_ASPECT_RATIO
                            val tLeft = getPointOnLine(vLeft, left.end, pHeight)
                            val tRight = getPointOnLine(vRight, right.end, pHeight)
                            foundPoints = arrayOf(vLeft, vRight, tLeft, tRight)
                        }
                    } else if (top != null && bottom != null && (left != null || right != null)) {
                        val vTop = if (left != null) left.intersect(top) else right!!.intersect(top)
                        val vBottom = if (left != null) left.intersect(bottom) else right!!.intersect(bottom)
                        Timber.d("got the edges")
                        if (vTop != null && vBottom != null) {
                            val pHeight = Line(vTop, vBottom).length()
                            val pWidth = pHeight * PASSPORT_ASPECT_RATIO
                            val tTop = getPointOnLine(vTop, top.end, pWidth)
                            val tBottom = getPointOnLine(vBottom, bottom.end, pWidth)
                            foundPoints = arrayOf(tTop, tBottom, vTop, vBottom)
                        }
                    }
                    if (foundPoints != null) {
                        val sPoints = sortPoints(foundPoints)
                        if (isInside(sPoints, newSize) && isLargeEnough(sPoints, Size(frameWidth, frameHeight), requiredCoverageRatio)) {
                            return Quadrilateral(null, sPoints)
                        } else
                            Timber.d("Not inside")
                    }
                }
            }
        }
        return null
    }

    private fun getPointOnLine(origin: Point, another: Point, distance: Double): Point {
        val dFactor = distance / Line(origin, another).length()
        val X = (1 - dFactor) * origin.x + dFactor * another.x
        val Y = (1 - dFactor) * origin.y + dFactor * another.y
        return Point(X, Y)
    }

    fun getQuadrilateral(contours: List<MatOfPoint>, srcSize: Size): Quadrilateral? {
        val ratio = getScaleRatio(srcSize)
        val height = java.lang.Double.valueOf(srcSize.height / ratio).toInt()
        val width = java.lang.Double.valueOf(srcSize.width / ratio).toInt()
        val size = Size(width.toDouble(), height.toDouble())
        for (c in contours) {
            val c2f = MatOfPoint2f(*c.toArray())
            val peri = Imgproc.arcLength(c2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
            val points = approx.toArray()

            // select biggest 4 angles polygon
            if (points.size == 4) {
                val foundPoints = sortPoints(points)
                val inside = isInside(foundPoints, size)
                val largeEnough = isLargeEnough(foundPoints, size, 0.25)
                if (inside && largeEnough) {
                    Timber.i("SCANNER found square inside and largeEnough")
                    return Quadrilateral(c, foundPoints)
                } else { //showToast(context, "Try getting closer to the ID");
                    Timber.v("SCANNER Not inside defined inside=$inside largeEnough=$largeEnough")
                }
            } else
                Timber.v("SCANNER approx size: ${points.size} do nothing, it's not squared")
        }
        //showToast(context, "Make sure the ID is on a contrasting background");
        return null
    }

    fun getQuadForPassport(contours: List<MatOfPoint>, srcSize: Size, frameSize: Int): Quadrilateral? {
        val requiredAspectRatio = 5
        val requiredCoverageRatio = 0.80f
        var rectContour: MatOfPoint? = null
        var foundPoints: Array<Point>? = null
        val ratio = getScaleRatio(srcSize)
        val width = java.lang.Double.valueOf(srcSize.width / ratio).toInt()
        val frameWidth = java.lang.Double.valueOf(frameSize / ratio).toInt()
        for (c in contours) {
            val bRect = Imgproc.boundingRect(c)
            val aspectRatio = bRect.width / bRect.height.toFloat()
            val coverageRatio = if (frameSize != 0) bRect.width / frameWidth.toFloat() else bRect.width / width.toFloat()
            Timber.d("AR: $aspectRatio, CR: $coverageRatio, frameWidth: $frameWidth")
            if (aspectRatio > requiredAspectRatio && coverageRatio > requiredCoverageRatio) {
                val c2f = MatOfPoint2f(*c.toArray())
                val peri = Imgproc.arcLength(c2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
                val points = approx.toArray()
                Timber.d("SCANNER approx size: ${points.size}")
                // select biggest 4 angles polygon
                if (points.size == 4) {
                    rectContour = c
                    foundPoints = sortPoints(points)
                    break
                } else if (points.size == 2) {
                    if (rectContour == null) {
                        rectContour = c
                        foundPoints = points
                    } else { //try to merge
                        val box1 = Imgproc.minAreaRect(MatOfPoint2f(*c.toArray()))
                        val box2 = Imgproc.minAreaRect(MatOfPoint2f(*rectContour.toArray()))
                        val ar = (box1.size.width / box2.size.width).toFloat()
                        if (box1.size.width > 0 && box2.size.width > 0 && 0.5 < ar && ar < 2.0) {
                            if (abs(box1.angle - box2.angle) <= 0.1 ||
                                abs(Math.PI - (box1.angle - box2.angle)) <= 0.1
                            ) {
                                val minAngle = Math.min(box1.angle, box2.angle)
                                val relX = box1.center.x - box2.center.x
                                val rely = box1.center.y - box2.center.y
                                val distance = Math.abs(rely * Math.cos(minAngle) - relX * Math.sin(minAngle))
                                if (distance < 1.5 * (box1.size.height + box2.size.height)) {
                                    val allPoints = Arrays.copyOf(foundPoints!!, 4)
                                    System.arraycopy(points, 0, allPoints, 2, 2)
                                    Timber.d("SCANNER after merge approx size: ${allPoints.size}")
                                    if (allPoints.size == 4) {
                                        foundPoints = sortPoints(allPoints)
                                        rectContour = MatOfPoint(*foundPoints)
                                        break
                                    }
                                }
                            }
                        }
                        rectContour = null
                        foundPoints = null
                    }
                }
            }
        }
        if (foundPoints != null && foundPoints.size == 4) {
            val lowerLeft = foundPoints[3]
            val lowerRight = foundPoints[2]
            val topLeft = foundPoints[0]
            var w = Math.sqrt((lowerRight.x - lowerLeft.x).pow(2.0) + (lowerRight.y - lowerLeft.y).pow(2.0))
            var h = Math.sqrt((topLeft.x - lowerLeft.x).pow(2.0) + (topLeft.y - lowerLeft.y).pow(2.0))
            var px = ((lowerLeft.x + w) * 0.03).toInt()
            var py = ((lowerLeft.y + h) * 0.03).toInt()
            lowerLeft.x = lowerLeft.x - px
            lowerLeft.y = lowerLeft.y + py
            px = ((lowerRight.x + w) * 0.03).toInt()
            py = ((lowerRight.y + h) * 0.03).toInt()
            lowerRight.x = lowerRight.x + px
            lowerRight.y = lowerRight.y + py
            val pRatio = 3.465f / 4.921f
            w = sqrt((lowerRight.x - lowerLeft.x).pow(2.0) + Math.pow(lowerRight.y - lowerLeft.y, 2.0))
            h = pRatio * w
            h = h - h * 0.04
            foundPoints[1] = Point(lowerRight.x, lowerRight.y - h)
            foundPoints[0] = Point(lowerLeft.x, lowerLeft.y - h)
            return Quadrilateral(rectContour, foundPoints)
        }
        return null
    }

    fun sortPoints(src: Array<Point>): Array<Point> {
        val srcPoints = ArrayList<Point>(listOf(*src))
        val result: Array<Point> = src.clone()
        val sumComparator = Comparator<Point> { lhs, rhs -> (lhs.y + lhs.x).compareTo(rhs.y + rhs.x) }
        val diffComparator = Comparator<Point> { lhs, rhs -> (lhs.y - lhs.x).compareTo(rhs.y - rhs.x) }
        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator)
        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator)
        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator)
        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator)
        return result
    }

    fun isInsideBaseArea(rp: Array<Point>, size: Size): Boolean {
        val width = java.lang.Double.valueOf(size.width).toInt()
        val height = java.lang.Double.valueOf(size.height).toInt()
        val baseMeasure = height / 4
        val bottomPos = height - baseMeasure
        val leftPos = width / 2 - baseMeasure
        val rightPos = width / 2 + baseMeasure
        return rp[0].x <= leftPos && rp[0].y <= baseMeasure && rp[1].x >= rightPos && rp[1].y <= baseMeasure && rp[2].x >= rightPos && rp[2].y >= bottomPos && rp[3].x <= leftPos && rp[3].y >= bottomPos
    }

    private fun isInside(points: Array<Point>, size: Size): Boolean {
        val width = java.lang.Double.valueOf(size.width).toInt()
        val height = java.lang.Double.valueOf(size.height).toInt()
        val isInside =
            points[0].x >= 0 && points[0].y >= 0 && points[1].x <= width && points[1].y >= 0 && points[2].x <= width && points[2].y <= height && points[3].x >= 0 && points[3].y <= height
        Timber.d("w: " + width + ", h: " + height + "Points: " + points[0] + ", " + points[1] + ", " + points[2] + ", " + points[3] + ", result: " + isInside)
        return isInside
    }

    private fun isLargeEnough(points: Array<Point>, size: Size, ratio: Double): Boolean {
        val contentWidth = Math.max(Line(points[0], points[1]).length(), Line(points[3], points[2]).length())
        val contentHeight = Math.max(Line(points[0], points[3]).length(), Line(points[1], points[2]).length())
        val widthRatio = contentWidth / size.width
        val heightRatio = contentHeight / size.height
        Timber.d("ratio: wr-" + widthRatio + ", hr-" + heightRatio + ", w: " + size.width + ", h: " + size.height + ", cw: " + contentWidth + ", ch: " + contentHeight)
        return widthRatio >= ratio && heightRatio >= ratio
    }

    fun getUpScaledPoints(points: Array<Point>, scaleFactor: Double): Array<Point> {
        val reScaledPoints = points.clone()
        for (i in 0..3) {
            val x = java.lang.Double.valueOf(points[i].x * scaleFactor).toInt()
            val y = java.lang.Double.valueOf(points[i].y * scaleFactor).toInt()
            reScaledPoints[i] = Point(x.toDouble(), y.toDouble())
        }
        return reScaledPoints
    }

    /**
     * @param src - actual image
     * @param pts - points scaled up with respect to actual image
     */
    fun fourPointTransform(src: Mat?, pts: Array<Point>): Mat {
        val tl = pts[0]
        val tr = pts[1]
        val br = pts[2]
        val bl = pts[3]
        val widthA = sqrt(Math.pow(br.x - bl.x, 2.0) + (br.y - bl.y).pow(2.0))
        val widthB = sqrt(Math.pow(tr.x - tl.x, 2.0) + (tr.y - tl.y).pow(2.0))
        val dw = Math.max(widthA, widthB)
        val maxWidth = java.lang.Double.valueOf(dw).toInt()
        val heightA = sqrt(Math.pow(tr.x - br.x, 2.0) + (tr.y - br.y).pow(2.0))
        val heightB = sqrt(Math.pow(tl.x - bl.x, 2.0) + (tl.y - bl.y).pow(2.0))
        val dh = max(heightA, heightB)
        val maxHeight = java.lang.Double.valueOf(dh).toInt()
        val doc = Mat(maxHeight, maxWidth, CvType.CV_8UC4)
        val srcMat = Mat(4, 1, CvType.CV_32FC2)
        val dstMat = Mat(4, 1, CvType.CV_32FC2)
        srcMat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)
        dstMat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh)
        val m = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        Imgproc.warpPerspective(src, doc, m, doc.size())
        return doc
    }

    fun adjustBrightnessAndContrast(src: Mat, clipPercentageIn: Double): Mat {
        var clipPercentage = clipPercentageIn
        val histSize = 256
        val alpha: Double
        val beta: Double
        var minGray: Double
        var maxGray: Double
        val gray: Mat
        if (src.type() == CvType.CV_8UC1) {
            gray = src.clone()
        } else {
            gray = Mat()
            Imgproc.cvtColor(src, gray, if (src.type() == CvType.CV_8UC3) Imgproc.COLOR_RGB2GRAY else Imgproc.COLOR_RGBA2GRAY)
        }
        if (clipPercentage == 0.0) {
            val minMaxGray = Core.minMaxLoc(gray)
            minGray = minMaxGray.minVal
            maxGray = minMaxGray.maxVal
        } else {
            val hist = Mat()
            val size = MatOfInt(histSize)
            val channels = MatOfInt(0)
            val ranges = MatOfFloat(0f, 256f)
            Imgproc.calcHist(listOf(gray), channels, Mat(), hist, size, ranges, false)
            gray.release()
            val accumulator = DoubleArray(histSize)
            accumulator[0] = hist[0, 0][0]
            for (i in 1 until histSize) {
                accumulator[i] = accumulator[i - 1] + hist[i, 0][0]
            }
            hist.release()
            val max = accumulator[accumulator.size - 1]
            clipPercentage = clipPercentage * (max / 100.0)
            clipPercentage = clipPercentage / 2.0f
            minGray = 0.0
            while (minGray < histSize && accumulator[minGray.toInt()] < clipPercentage) {
                minGray++
            }
            maxGray = histSize - 1.toDouble()
            while (maxGray >= 0 && accumulator[maxGray.toInt()] >= max - clipPercentage) {
                maxGray--
            }
        }
        val inputRange = maxGray - minGray
        alpha = (histSize - 1) / inputRange
        beta = -minGray * alpha
        val result = Mat()
        src.convertTo(result, -1, alpha, beta)
        if (result.type() == CvType.CV_8UC4) {
            Core.mixChannels(Arrays.asList(src), Arrays.asList(result), MatOfInt(3, 3))
        }
        return result
    }

    fun sharpenImage(src: Mat?): Mat {
        val sharped = Mat()
        Imgproc.GaussianBlur(src, sharped, Size(0.0, 0.0), 3.0)
        Core.addWeighted(src, 1.5, sharped, -0.5, 0.0, sharped)
        return sharped
    }

    class Quadrilateral internal constructor(var contour: MatOfPoint?, var points: Array<Point>)
}