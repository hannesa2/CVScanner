package info.hannes.cvscanner.util

import org.opencv.core.Point
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class Line(val start: Point, val end: Point) {

    private var slope = INFINITE_SLOPE.toDouble()

    constructor(x1: Double, y1: Double, x2: Double, y2: Double) : this(Point(x1, y1), Point(x2, y2))

    init {
        findSlope()
    }

    private fun findSlope() {
        if (start.x == end.x)
            return
        slope = abs((end.y - start.y) / (end.x - start.x))
    }

    fun length(): Double {
        return sqrt(Math.pow(start.x - end.x, 2.0) + (start.y - end.y).pow(2.0))
    }

    fun isInleft(width: Double): Boolean {
        return Math.max(start.x, end.x) < width / 2.0
    }

    fun isInBottom(height: Double): Boolean {
        return Math.max(start.y, end.y) > height / 2.0
    }

    fun intersect(line: Line): Point? {
        val denominator = (start.x - end.x) * (line.start.y - line.end.y) - (line.start.x - line.end.x) * (start.y - end.y)
        if (denominator > THRESHOLD) {
            val x =
                ((start.x * end.y - start.y * end.x) * (line.start.x - line.end.x) - (line.start.x * line.end.y - line.start.y * line.end.x) * (start.x - end.x)) / denominator
            val y =
                ((start.x * end.y - start.y * end.x) * (line.start.y - line.end.y) - (line.start.x * line.end.y - line.start.y * line.end.x) * (start.y - end.y)) / denominator
            return Point(x, y)
        }
        return null
    }

    fun merge(line: Line): Line? {
        val DIFF_THRESHOLD = 40
        if (isNearHorizontal && line.isNearHorizontal) {
            var fLine = this
            var sLine = line
            if (start.x > line.start.x) {
                fLine = line
                sLine = this
            }
            val yDiff = Math.abs(
                Math.min(
                    Math.min(fLine.start.y - sLine.start.y, fLine.start.y - sLine.end.y),
                    Math.min(fLine.end.y - sLine.end.y, fLine.end.y - sLine.start.y)
                )
            )
            if (yDiff < DIFF_THRESHOLD) {
                return merge(Arrays.asList(fLine.start, fLine.end, sLine.start, sLine.end), true)
            }
        } else if (isNearVertical && line.isNearVertical) {
            var fLine = this
            var sLine = line
            if (start.y > line.start.y) {
                fLine = line
                sLine = this
            }
            //double slopeDiff = Math.abs(fLine.slope - sLine.slope);
            val xDiff =
                abs(min(min(fLine.start.x - sLine.start.x, fLine.start.x - sLine.end.x), min(fLine.end.x - sLine.end.x, fLine.end.x - sLine.start.x)))
            if (xDiff < DIFF_THRESHOLD) {
                return merge(listOf(fLine.start, fLine.end, sLine.start, sLine.end), false)
            }
        }
        return null
    }

    private fun merge(points: List<Point>, isHorizontal: Boolean): Line {
        Collections.sort(points) { o1, o2 -> (if (isHorizontal) o1.x - o2.x else o1.y - o2.y).toInt() }
        return Line(points[0], points[points.size - 1])
    }

    val isNearVertical: Boolean
        get() = slope == INFINITE_SLOPE.toDouble() || slope > 5.671

    val isNearHorizontal: Boolean
        get() = slope < 0.176

    companion object {

        private val INFINITE_SLOPE = Float.MAX_VALUE
        private const val THRESHOLD = 0.1f

        fun joinSegments(segments: List<Line>): List<Line> {
            val stack: Deque<Line> = ArrayDeque<Line>()
            stack.push(segments[0])
            for (i in 1 until segments.size) {
                val second = segments[i]
                val first = stack.peek()
                val merged = first!!.merge(second)
                if (merged != null) {
                    stack.pop()
                    stack.push(merged)
                } else stack.push(second)
            }
            return ArrayList(stack)
        }
    }

}