package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class GLShader {

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

    public void init(String vertexShaderSrc, String fragmentShaderSrc) {
        GL2 gl = GLUtils.getGL2();

        int vertexShaderId = compileShader(vertexShaderSrc, GL2.GL_VERTEX_SHADER);
        int fragmentShaderId = compileShader(fragmentShaderSrc, GL2.GL_FRAGMENT_SHADER);

        gl.glAttachShader(programId, vertexShaderId);
        GLUtils.checkError("glAttachShader");

        gl.glAttachShader(programId, fragmentShaderId);
        GLUtils.checkError("glAttachShader");

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
    }

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
            gl.glBindBuffersBase(GL4.GL_UNIFORM_BUFFER, entry.getKey(), 1, new int[]{ entry.getValue().getGLBufferId() }, 0);
            GLUtils.checkError("glBindBuffersBase");
        }

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

    public void addUniformBuffer(int binding, GLUniformBuffer buffer) {
        uniformBuffers.put(binding, buffer);
    }

    public void addTexture(int binding, GLTexture texture) {
        textures.put(binding, texture);
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
