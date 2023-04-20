package baka.chaomian.fractalwp

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

@SuppressLint("ViewConstructor")
class GLWallpaperSurface private constructor(
    context: Context,
    val renderer: FractalRenderer
) : GLSurfaceView(context.applicationContext) {

    companion object {
        private val initEngine = ThreadLocal<WallpaperService.Engine?>()

        operator fun invoke(
            context: Context,
            engine: WallpaperService.Engine,
            renderer: FractalRenderer
        ): GLWallpaperSurface {
            initEngine.set(engine)
            try {
                return GLWallpaperSurface(context, renderer)
            } finally {
                initEngine.set(null)
            }
        }
    }

    private val engine = initEngine.get()!!

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun getHolder(): SurfaceHolder = (initEngine.get() ?: engine).surfaceHolder
}
