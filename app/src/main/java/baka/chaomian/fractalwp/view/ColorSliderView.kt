package baka.chaomian.fractalwp.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val touchRadiusDp = 10f
    }

    private val rect = RectF()
    private val saturationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
    }

    var hue: Float = 0f
        set(value) {
            field = value
            updateGradient()
        }

    var saturation: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                updateGradient()
            }
        }

    var value: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateGradient(w, h)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    private fun updateGradient() {
        val width = width
        val height = height
        if (width > 0 && height > 0) {
            updateGradient(width, height)
        }
    }

    private fun updateGradient(width: Int, height: Int) {
        val colors = intArrayOf(Color.HSVToColor(floatArrayOf(hue, saturation, 1f)), Color.BLACK)
        saturationPaint.shader = LinearGradient(
            paddingLeft.toFloat(), paddingBottom.toFloat(), paddingLeft.toFloat() + width,
            paddingBottom.toFloat() + height, colors, null, Shader.TileMode.CLAMP
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat()
        )
        val touchRadius = touchRadiusDp * resources.displayMetrics.density
        val rounding = width - paddingRight - paddingLeft.toFloat()
        canvas.drawRoundRect(rect, rounding, rounding, saturationPaint)
        val y = height - paddingBottom - value * (height - paddingTop - paddingBottom)
        canvas.drawCircle(width / 2f, y, touchRadius, circlePaint)
        super.onDraw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val eventY = event.y
                value = (1f - (eventY - paddingTop) / (height - paddingTop - paddingBottom)).coerceIn(0f, 1f)
                callOnClick()
            }
        }
        return true
    }
}
