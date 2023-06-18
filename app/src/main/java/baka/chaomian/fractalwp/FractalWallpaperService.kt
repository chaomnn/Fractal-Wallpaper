package baka.chaomian.fractalwp

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.preference.PreferenceManager
import kotlin.math.asin

class FractalWallpaperService : WallpaperService() {

    companion object {
        private const val moveModeKey = "move_mode"
        private const val colorModeKey = "color_switch"
        private const val outColorKey = "out_color"
        private const val constKey = "current_constant"
        private const val coloringMethodKey = "coloring_method"
        private const val scaleFactorKey = "scaleFactor"
        private const val iterationCountKey = "iteration_count"

        private const val scaleRatio = 0.02f

        private val handler = Handler(Looper.getMainLooper())
    }

    override fun onCreateEngine(): Engine = FractalWallpaperEngine()

    inner class FractalWallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private lateinit var surface: GLWallpaperSurface<FractalRenderer>
        private var size = Size(0, 0)
        private var prevEventTime = 0L
        private var moveModeOn = false
        private val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        private var doubleTapSetting = preferences.getBoolean(moveModeKey, false)

        private var scaleFactor = 1f
        private var zoomModeOn = false
        private val scaleDetector: ScaleGestureDetector = ScaleGestureDetector(applicationContext,
            object: ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (moveModeOn) {
                        return false
                    }
                    scaleFactor += if (detector.scaleFactor < 1f) scaleRatio else -scaleRatio
                    scaleFactor = scaleFactor.coerceIn(0.3f, 2.5f)
                    surface.renderer.scaleFactor = scaleFactor
                    preferences.edit().putFloat(scaleFactorKey, scaleFactor).apply()
                    surface.requestRender()
                    return super.onScale(detector)
                }
            })

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            surface = GLWallpaperSurface(applicationContext, surfaceHolder, FractalRenderer(applicationContext))
            preferences.registerOnSharedPreferenceChangeListener(this)
            surface.renderer.colorSwitchMode = preferences.getBoolean(colorModeKey, false)
            surface.renderer.color = getColorArray(preferences.getInt(outColorKey, Color.WHITE))
            surface.renderer.useLogColor = preferences.getBoolean(coloringMethodKey, false)
            surface.renderer.scaleFactor = preferences.getFloat(scaleFactorKey, 1f)
        }

        override fun onDestroy() {
            super.onDestroy()
            preferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            size = Size(width, height)
        }

        private val disableMoveMode = Runnable { moveModeOn = false }
        private val disableZoomMode = Runnable { zoomModeOn = false }

        override fun onTouchEvent(event: MotionEvent) {
            scaleDetector.onTouchEvent(event)
            super.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    handler.removeCallbacks(disableZoomMode)
                    zoomModeOn = true
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    handler.postDelayed(disableZoomMode, 300L)
                }

                MotionEvent.ACTION_DOWN -> if (doubleTapSetting) {
                    if (prevEventTime != 0L &&
                        event.eventTime - prevEventTime < ViewConfiguration.getDoubleTapTimeout()
                    ) {
                        handler.removeCallbacks(disableMoveMode)
                        moveModeOn = true
                    }
                    prevEventTime = event.eventTime
                }

                MotionEvent.ACTION_UP -> if (moveModeOn) {
                    handler.postDelayed(disableMoveMode, 300L)
                }
            }
            if ((doubleTapSetting && !moveModeOn) || zoomModeOn) {
                return
            }
            if (size.width > 0 && size.height > 0) {
                val portrait = size.width < size.height
                val xGL = ((2f * event.x / size.width) - 1f) *
                        (if (portrait) size.width.toFloat() / size.height else 1f) * scaleFactor
                val yGL = (1f - (2f * event.y) / size.height) *
                        (if (portrait) 1f else size.height.toFloat() / size.width) * scaleFactor
                surface.renderer.juliaConstants = floatArrayOf(xGL, yGL)
                preferences.edit()
                    .putString(constKey, if (yGL > 0) "${xGL} +${yGL}i" else "${xGL} ${yGL}i")
                    .apply()
                surface.requestRender()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                val constString = preferences.getString(constKey, "")!!
                if (constString.isNotEmpty() && constString.contains(" ")) {
                    val list = constString.split(" ")
                    val y = list[1].dropLast(1)
                    surface.renderer.juliaConstants = floatArrayOf(list[0].toFloat(),
                        if (y.startsWith("+")) y.drop(1).toFloat() else y.toFloat())
                }
                surface.onResume()
            } else {
                surface.onPause()
            }
        }

        private fun getColorArray(color: Int): FloatArray {
            val rgba = floatArrayOf(
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f,
                Color.alpha(color) / 255f
            )
            return FloatArray(rgba.size) { asin(rgba[it]) }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                moveModeKey ->
                    doubleTapSetting = preferences.getBoolean(moveModeKey, false)

                colorModeKey ->
                    surface.renderer.colorSwitchMode = preferences.getBoolean(colorModeKey, false)

                outColorKey -> {
                    surface.renderer.color = getColorArray(preferences.getInt(outColorKey, Color.BLUE))
                }

                coloringMethodKey -> {
                    surface.renderer.useLogColor = preferences.getBoolean(coloringMethodKey, false)
                }

                iterationCountKey -> {
                    surface.renderer.iterationLimit = Integer.parseInt(preferences.getString(iterationCountKey, "500")!!)
                }
            }
        }
    }
}
