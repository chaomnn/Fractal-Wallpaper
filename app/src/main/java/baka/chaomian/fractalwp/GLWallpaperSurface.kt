package baka.chaomian.fractalwp

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.SurfaceHolder

@SuppressLint("ViewConstructor")
class GLWallpaperSurface<T : GLSurfaceView.Renderer> private constructor(
    context: Context,
    val renderer: T
) : GLSurfaceView(context.applicationContext) {

    companion object {
        private val initHolder = ThreadLocal<SurfaceHolder?>()

        operator fun <T : Renderer> invoke(
            context: Context,
            holder: SurfaceHolder,
            renderer: T
        ): GLWallpaperSurface<T> {
            initHolder.set(holder)
            try {
                return GLWallpaperSurface(context, renderer)
            } finally {
                initHolder.set(null)
            }
        }
    }

    private val holder = initHolder.get()!!

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun getHolder(): SurfaceHolder = initHolder.get() ?: holder
}
