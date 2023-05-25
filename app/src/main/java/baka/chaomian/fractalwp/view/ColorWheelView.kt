package baka.chaomian.fractalwp.view;

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private val hueColors = intArrayOf(
            Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN
        )
        private val lightnessColors = intArrayOf(
            Color.WHITE, Color.TRANSPARENT
        )
        private const val touchRadiusDp = 10f
    }

    private val centerX get() = width / 2f
    private val centerY get() = height / 2f

    private val radius: Float
        get() = minOf(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom) / 2f

    var hue: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var saturation: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lightnessPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateShaders(right - left, bottom - top)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShaders(w, h)
    }

    private fun updateShaders(w: Int, h: Int) {
        huePaint.shader = SweepGradient(w / 2f, h / 2f, hueColors, null)
        lightnessPaint.shader = RadialGradient(w / 2f, h / 2f, radius, lightnessColors, null, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val centerX = centerX
        val centerY = centerY
        val radius = radius
        val touchRadius = touchRadiusDp * resources.displayMetrics.density
        canvas.drawCircle(centerX, centerY, radius, huePaint)
        canvas.drawCircle(centerX, centerY, radius, lightnessPaint)

        val length = saturation * radius
        val angle = Math.toRadians(hue.toDouble() - 180).toFloat()
        val x = if (length != 0f) length * cos(angle) + centerX else centerX
        val y = if (length != 0f) length * sin(angle) + centerY else centerY
        canvas.drawCircle(x, y, touchRadius, circlePaint)
        super.onDraw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x - centerX
                val y = event.y - centerY
                val length = sqrt(x.pow(2) + y.pow(2))
                val r = minOf(radius, length)

                val eventX = r * x / length
                val eventY = r * y / length
                hue = Math.toDegrees(atan2(eventY, eventX) + kotlin.math.PI).toFloat()
                saturation = sqrt((eventX).pow(2) + (eventY).pow(2)) / radius
                callOnClick()
            }
        }
        return true
    }
}
