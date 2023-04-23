package baka.chaomian.fractalwp

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Size
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import androidx.preference.PreferenceManager

class FractalWallpaperService : WallpaperService() {

    companion object {
        private const val moveModeKey = "move_mode"
        private const val colorModeKey = "color_switch"
        private val colorKeys = arrayOf("red", "green", "blue", "alpha")

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
            surface.renderer.boundedColor = getBoundedColor()
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

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                moveModeKey ->
                    doubleTapSetting = preferences.getBoolean(moveModeKey, false)
                colorModeKey ->
                    surface.renderer.colorSwitchMode = preferences.getBoolean(colorModeKey, false)
                in colorKeys ->
                    surface.renderer.boundedColor = getBoundedColor()
            }
        }

        private fun getBoundedColor() = FloatArray(colorKeys.size) { index ->
            val key = colorKeys[index]
            val isAlpha = key == "alpha"
            preferences.getInt(key, if (isAlpha) 0 else 255) / if (isAlpha) 100f else 255f
        }
    }
}
