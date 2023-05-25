package baka.chaomian.fractalwp

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Size
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.preference.PreferenceManager
import kotlin.math.asin

class FractalWallpaperService : WallpaperService() {

    companion object {
        private const val moveModeKey = "move_mode"
        private const val colorModeKey = "color_switch"
        private const val boundedColorKey = "bounded_color"
        private const val outColorKey = "out_color"

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

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            surface = GLWallpaperSurface(applicationContext, surfaceHolder, FractalRenderer(applicationContext))
            preferences.registerOnSharedPreferenceChangeListener(this)
            surface.renderer.colorSwitchMode = preferences.getBoolean(colorModeKey, false)
            surface.renderer.boundedColor = getColorArray(preferences.getInt(boundedColorKey, Color.WHITE), false)
            surface.renderer.color = getColorArray(preferences.getInt(outColorKey, Color.WHITE), true)
        }

        override fun onDestroy() {
            super.onDestroy()
            preferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            size = Size(width, height)
        }

        private val disableMoveMode = Runnable { moveModeOn = false }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            when (event.action) {
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
            if (doubleTapSetting && !moveModeOn) {
                return
            }
            if (size.width > 0 && size.height > 0) {
                val portrait = size.width < size.height
                val xGL = ((2f * event.x / size.width) - 1f) *
                        (if (portrait) size.width.toFloat() / size.height else 1f)
                val yGL = (1f - (2f * event.y) / size.height) *
                        (if (portrait) 1f else size.height.toFloat() / size.width)
                println("xGL $xGL yGL $yGL")
                surface.renderer.juliaConstants = floatArrayOf(xGL, yGL)
                surface.requestRender()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                surface.onResume()
            } else {
                surface.onPause()
            }
        }

        private fun getColorArray(color: Int, outColor: Boolean): FloatArray {
            val rgba = floatArrayOf(
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f,
                Color.alpha(color) / 255f
            )
            return if (outColor) FloatArray(rgba.size) { asin(rgba[it]) } else rgba
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                moveModeKey ->
                    doubleTapSetting = preferences.getBoolean(moveModeKey, false)

                colorModeKey ->
                    surface.renderer.colorSwitchMode = preferences.getBoolean(colorModeKey, false)

                boundedColorKey -> {
                    surface.renderer.boundedColor = getColorArray(
                        preferences.getInt(boundedColorKey, Color.WHITE), false
                    )
                }

                outColorKey -> {
                    surface.renderer.color = getColorArray(preferences.getInt(outColorKey, Color.BLUE), true)
                }
            }
        }
    }
}
