package baka.chaomian.fractalwp

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FractalRenderer(private val context: Context) : Renderer {

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

    private var transformMatrixId = 0
    private var zoomMatrixId = 0
    private var cId = 0
    private var colorId = 0
    private var useLogColorId = 0

    private var width = 0
    private var height = 0
    private var glProgram = 0
    lateinit var color : FloatArray
    var colorSwitchMode = false
    var useLogColor = false
    var juliaConstants = floatArrayOf(0.395f, -0.159f)
    var scaleFactor = 1f

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
        glProgram = GLES30.glCreateProgram().also { program ->
            GLES30.glAttachShader(
                program,
                loadShader(GLES30.GL_VERTEX_SHADER, context.resources.openRawResource(R.raw.vertex))
            )
            GLES30.glAttachShader(
                program,
                loadShader(GLES30.GL_FRAGMENT_SHADER, context.resources.openRawResource(R.raw.fragment))
            )
            GLES30.glLinkProgram(program)
            GLES30.glUseProgram(program)
        }

        // Enable buffer
        buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .apply { position(0) }
        GLES30.glGetAttribLocation(glProgram, "pos").let { location ->
            GLES30.glEnableVertexAttribArray(location)
            GLES30.glVertexAttribPointer(location, 2, GLES30.GL_FLOAT, false, 0, buffer)
        }

        // Get locations of uniform variables
        transformMatrixId = GLES30.glGetUniformLocation(glProgram, "transformMat")
        cId = GLES30.glGetUniformLocation(glProgram, "constNum")
        zoomMatrixId = GLES30.glGetUniformLocation(glProgram, "zoomMat")
        colorId = GLES30.glGetUniformLocation(glProgram, "baseColor")
        useLogColorId = GLES30.glGetUniformLocation(glProgram, "useLogColor")

        // Clear color
        GLES30.glClearColor(0f, 0f, 0f, 1f)

        // Set constants for polynomial
        GLES30.glUniform2fv(cId, 1, juliaConstants, 0)

        // Identity matrices
        Matrix.setIdentityM(transformationMatrix, 0)
        Matrix.setIdentityM(zoomMatrix, 0)
        Matrix.setIdentityM(identityMatrix, 0)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        this.width = width
        this.height = height

        // Transformation matrix
        val portrait = width < height
        Matrix.scaleM(
            transformationMatrix, 0, identityMatrix, 0,
            if (portrait) height.toFloat() / width else 1f,
            if (portrait) 1f else width.toFloat() / height,
            1f
        )
        val scaleFactor = if (portrait) {
            (height.toFloat() / width) * scaleFactor
        } else {
            (width.toFloat() / height) * scaleFactor
        }
        Matrix.scaleM(zoomMatrix, 0, identityMatrix, 0, scaleFactor, scaleFactor, 1f)
    }

    override fun onDrawFrame(unused: GL10?) {
        // Set matrices and color
        GLES30.glUniformMatrix4fv(transformMatrixId, 1, false, transformationMatrix, 0)
        if (scaleFactor != 1f) {
            val scaleFactor = if (width < height) {
                (height.toFloat() / width) * scaleFactor
            } else {
                (width.toFloat() / height) * scaleFactor
            }
            Matrix.scaleM(zoomMatrix, 0, identityMatrix, 0, scaleFactor, scaleFactor, 1f)
        }
        GLES30.glUniformMatrix4fv(zoomMatrixId, 1, false, zoomMatrix, 0)
        GLES30.glUniform2fv(cId, 1, juliaConstants, 0)
        GLES30.glUniform1i(useLogColorId, if (useLogColor) 1 else 0)
        if (colorSwitchMode) {
            color = FloatArray(color.size) { color[it] + 0.025f }
        }
        GLES30.glUniform4fv(colorId, 1, color, 0)

        // Draw
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }
}
