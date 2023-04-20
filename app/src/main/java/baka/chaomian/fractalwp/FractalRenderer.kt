package baka.chaomian.fractalwp

import android.content.Context
import android.content.SharedPreferences
import android.opengl.GLES30
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import androidx.preference.PreferenceManager
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FractalRenderer(private val context: Context) : Renderer, SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        val defaultColor = floatArrayOf(3.4f, 3.9f, 5.0f)
        val colorKeys = arrayOf("red", "green", "blue", "alpha")
        const val colorModeKey = "color_switch"
    }

    private val vertices = floatArrayOf(
        -1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, -1.0f,
        1.0f, 1.0f
    )

    private lateinit var buffer : FloatBuffer

    private val transformationMatrix = FloatArray(16)
    private val zoomMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var transformMatrixId = 0
    private var zoomMatrixId = 0
    private var cId = 0
    private var colorId = 0
    private var boundedColorId = 0

    private var color = defaultColor
    private var glProgram = 0
    private var colorSwitchMode = false
    private lateinit var boundedColor : FloatArray
    var juliaConstants = floatArrayOf(0.273f, 0.005f)

    private fun loadShader(type: Int, source: InputStream): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, source.use {
                it.readBytes().toString(Charsets.UTF_8)
            })
            GLES30.glCompileShader(shader)
            val compileCheck = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileCheck, 0)
            if (compileCheck[0] == 0) {
                val errorLog = GLES30.glGetShaderInfoLog(shader)
                throw RuntimeException(errorLog)
            }
        }
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // Create program
        glProgram = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(
                it,
                loadShader(GLES30.GL_VERTEX_SHADER, context.resources.openRawResource(R.raw.vertex))
            )
            GLES30.glAttachShader(
                it,
                loadShader(GLES30.GL_FRAGMENT_SHADER, context.resources.openRawResource(R.raw.fragment))
            )
            GLES30.glLinkProgram(it)
            GLES30.glUseProgram(it)
        }

        // Enable buffer
        buffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }
        GLES30.glGetAttribLocation(glProgram, "pos").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(it, 2, GLES30.GL_FLOAT, false, 0, buffer)
        }

        // Get locations of uniform variables
        transformMatrixId = GLES30.glGetUniformLocation(glProgram, "transformMat")
        cId = GLES30.glGetUniformLocation(glProgram, "constNum")
        zoomMatrixId = GLES30.glGetUniformLocation(glProgram, "zoomMat")
        colorId = GLES30.glGetUniformLocation(glProgram, "baseColor")
        boundedColorId = GLES30.glGetUniformLocation(glProgram, "boundedColor")

        // Clear color
        GLES30.glClearColor(0f, 0f, 0f, 1f)

        // Set constants for polynomial
        GLES30.glUniform2fv(cId, 1, juliaConstants, 0)

        // Identity matrices
        Matrix.setIdentityM(transformationMatrix, 0)
        Matrix.setIdentityM(zoomMatrix, 0)
        Matrix.setIdentityM(identityMatrix, 0)

        boundedColor = colorKeys.map {
            preferences.getInt(it, 0) / if (it != "alpha") 1f else 100f
        }.toFloatArray()
        colorSwitchMode = preferences.getBoolean(colorModeKey, false)
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)

        // Transformation matrix
        val portrait = width < height
        Matrix.scaleM(
            transformationMatrix, 0, identityMatrix, 0, if (portrait) height.toFloat() / width else 1f,
            if (portrait) 1f else width.toFloat() / height, 1f
        )
        val scaleFactor = if (portrait) height.toFloat() / width else width.toFloat() / height
        Matrix.scaleM(zoomMatrix, 0, identityMatrix, 0, scaleFactor, scaleFactor, 1f)
    }

    override fun onDrawFrame(unused: GL10?) {
        // Set matrices and color
        GLES30.glUniformMatrix4fv(transformMatrixId, 1, false, transformationMatrix, 0)
        GLES30.glUniformMatrix4fv(zoomMatrixId, 1, false, zoomMatrix, 0)
        GLES30.glUniform4fv(boundedColorId, 1, boundedColor, 0)
        GLES30.glUniform2fv(cId, 1, juliaConstants, 0)
        if (colorSwitchMode) {
            color = color.map {
                it + 0.05f
            }.toFloatArray()
        }
        GLES30.glUniform3fv(colorId, 1, color, 0)

        // Draw
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (colorKeys.contains(key)) {
            boundedColor = colorKeys.map {
                preferences.getInt(it, 0) / if (it != "alpha") 1f else 100f
            }.toFloatArray()
        } else if (key.equals(colorModeKey)) {
            colorSwitchMode = preferences.getBoolean(colorModeKey, false)
        }
    }
}
