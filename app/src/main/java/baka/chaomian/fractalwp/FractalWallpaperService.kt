package baka.chaomian.fractalwp

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.util.Size
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager

class FractalWallpaperService : WallpaperService() {

    companion object {
        const val moveModeKey = "move_mode"
    }

    override fun onCreateEngine(): Engine = FractalWallpaperEngine()

    inner class FractalWallpaperEngine : Engine(), OnSharedPreferenceChangeListener {

        private lateinit var surface: GLWallpaperSurface
        private var size = Size(0, 0)
        private var prevEventTime = 0L
        private var moveModeOn = false
        private val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        private var doubleTapSetting = preferences.getBoolean(moveModeKey, false)

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            surface = GLWallpaperSurface(applicationContext, this, FractalRenderer(applicationContext))
            preferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            size = Size(width, height)
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> if (doubleTapSetting) {
                    if (prevEventTime != 0L && event.eventTime - prevEventTime < 200) {
                        moveModeOn = true
                    }
                    prevEventTime = event.eventTime
                }
                MotionEvent.ACTION_UP -> if (moveModeOn) {
                    Handler(mainLooper).postDelayed({ moveModeOn = false }, 300)
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
            if (key.equals(moveModeKey)) {
                doubleTapSetting = preferences.getBoolean(moveModeKey, false)
            }
        }
    }
}
