package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import org.joml.Vector2fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class GLShader implements GLObject {

    private int programId;
    private Map<Integer, GLUniformBuffer> uniformBuffers;
    private Map<Integer, GLTexture> textures;

    public GLShader() {
        GL2 gl = GLUtils.getGL2();

        this.programId = gl.glCreateProgram();
        GLUtils.checkError("glCreateProgram");

        this.uniformBuffers = new HashMap<>();
        this.textures = new HashMap<>();
    }

    public void init(String vertexShaderSrc, String geometryShaderSrc, String fragmentShaderSrc) {
        GL2 gl = GLUtils.getGL2();

        int vertexShaderId = compileShader(vertexShaderSrc, GL2.GL_VERTEX_SHADER);
        gl.glAttachShader(programId, vertexShaderId);
        GLUtils.checkError("glAttachShader");

        int fragmentShaderId = compileShader(fragmentShaderSrc, GL2.GL_FRAGMENT_SHADER);
        gl.glAttachShader(programId, fragmentShaderId);
        GLUtils.checkError("glAttachShader");

        int geometryShaderId = 0;
        if(geometryShaderSrc != null) {
            geometryShaderId = compileShader(geometryShaderSrc, GL3.GL_GEOMETRY_SHADER);
            gl.glAttachShader(programId, geometryShaderId);
            GLUtils.checkError("glAttachShader");
        }

        gl.glLinkProgram(programId);
        GLUtils.checkError("glLinkProgram");

        IntBuffer intBuffer = Buffers.newDirectIntBuffer(1);
        gl.glGetProgramiv(programId, GL2.GL_LINK_STATUS, intBuffer);
        GLUtils.checkError("glGetProgramiv");
        if(intBuffer.get(0) == GL.GL_FALSE) {
            gl.glGetProgramiv(programId, GL2.GL_INFO_LOG_LENGTH, intBuffer);
            GLUtils.checkError("glGetProgramiv");
            int length = intBuffer.get(0);
            if(length > 0) {
                ByteBuffer infoLog = Buffers.newDirectByteBuffer(length);
                gl.glGetProgramInfoLog(programId, infoLog.limit(), intBuffer, infoLog);
                GLUtils.checkError("glGetProgramInfoLog");
                byte[] infoLogData = new byte[length];
                infoLog.get(infoLogData);
                GLUtils.logError("Shader link error: " + new String(infoLogData));
            }
        }

        gl.glDeleteShader(vertexShaderId);
        GLUtils.checkError("glDeleteShader");

        gl.glDeleteShader(fragmentShaderId);
        GLUtils.checkError("glDeleteShader");

        if(geometryShaderSrc != null) {
            gl.glDeleteShader(geometryShaderId);
            GLUtils.checkError("glDeleteShader");
        }
    }

    @Override
    public void dispose() {
        GL2 gl = GLUtils.getGL2();

        if(programId != 0) {
            gl.glDeleteProgram(programId);
            GLUtils.checkError("glDeleteProgram");
        }
    }

    public void bind() {
        GL4 gl = GLUtils.getGL4();

        gl.glUseProgram(programId);
        GLUtils.checkError("glUseProgram");

        for(Map.Entry<Integer, GLUniformBuffer> entry : uniformBuffers.entrySet()) {
            gl.glBindBufferBase(GL4.GL_UNIFORM_BUFFER, entry.getKey(), entry.getValue().getGLBufferId());
            GLUtils.checkError("glBindBuffersBase");
        }

        updateTextures();
    }

    public void updateTextures() {
        GL4 gl = GLUtils.getGL4();

        int activeIndex = 0;
        for(Map.Entry<Integer, GLTexture> entry : textures.entrySet()) {
            gl.glActiveTexture(GL.GL_TEXTURE0 + activeIndex);
            GLUtils.checkError("glActiveTexture");
            entry.getValue().bind();
            gl.glUniform1i(entry.getKey(), activeIndex);
            // TODO: Fix texture binding spitting out invalid operation
            // GLUtils.checkError("glUniform1i");
            gl.glGetError();
            if(entry.getValue().getSampler() != null) {
                gl.glBindSampler(activeIndex, entry.getValue().getSampler().getGLSamplerId());
                GLUtils.checkError("glBindSampler");
            }
            activeIndex++;
        }
    }

    public void unbind() {
        GL2 gl = GLUtils.getGL2();

        gl.glUseProgram(0);
        GLUtils.checkError("glUseProgram");
    }

    public void setUniform1f(String name, float value) {
        GL2 gl = GLUtils.getGL2();

        int location = gl.glGetUniformLocation(programId, name);
        GLUtils.checkError("glGetUniformLocation");

        gl.glUniform1f(location, value);
        GLUtils.checkError("glUniform1f");
    }

    public void setUniform2f(String name, Vector2fc value) {
        GL2 gl = GLUtils.getGL2();

        int location = gl.glGetUniformLocation(programId, name);
        GLUtils.checkError("glGetUniformLocation");

        gl.glUniform2f(location, value.x(), value.y());
        GLUtils.checkError("glUniform2f");
    }

    public void setUniform4f(String name, Vector4fc value) {
        GL2 gl = GLUtils.getGL2();

        int location = gl.glGetUniformLocation(programId, name);
        GLUtils.checkError("glGetUniformLocation");

        gl.glUniform4f(location, value.x(), value.y(), value.z(), value.w());
        //GLUtils.checkError("glUniform4f");
        gl.glGetError();
    }

    public void addUniformBuffer(int binding, GLUniformBuffer buffer) {
        uniformBuffers.put(binding, buffer);
    }

    public void addTexture(String name, GLTexture texture) {
        GL2 gl = GLUtils.getGL2();

        int location = gl.glGetUniformLocation(programId, name);
        GLUtils.checkError("glGetUniformLocation");

        textures.put(location, texture);
    }

    private int compileShader(String shaderSrc, int shaderType) {
        GL2 gl = GLUtils.getGL2();

        int shaderId = gl.glCreateShader(shaderType);
        GLUtils.checkError("glCreateShader");

        gl.glShaderSource(shaderId, 1, new String[]{ shaderSrc }, null);
        GLUtils.checkError("glShaderSource");

        gl.glCompileShader(shaderId);
        GLUtils.checkError("glCompileShader");

        IntBuffer intBuffer = Buffers.newDirectIntBuffer(1);
        gl.glGetShaderiv(shaderId, GL2.GL_COMPILE_STATUS, intBuffer);
        GLUtils.checkError("glGetShaderiv");
        if(intBuffer.get(0) == GL.GL_FALSE) {
            gl.glGetShaderiv(shaderId, GL2.GL_INFO_LOG_LENGTH, intBuffer);
            GLUtils.checkError("glGetShaderiv");
            int length = intBuffer.get(0);
            if(length > 0) {
                ByteBuffer infoLog = Buffers.newDirectByteBuffer(length);
                gl.glGetShaderInfoLog(shaderId, infoLog.limit(), intBuffer, infoLog);
                GLUtils.checkError("glGetShaderInfoLog");
                byte[] infoLogData = new byte[length];
                infoLog.get(infoLogData);
                GLUtils.logError("Shader compile error: " + new String(infoLogData));
            }
        }

        return shaderId;
    }
}
