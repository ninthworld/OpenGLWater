package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import utils.DataFormat;

import java.nio.*;

public class GLUniformBuffer {

    private int bufferId;

    public GLUniformBuffer() {
        GL gl = GLUtils.getGL();

        IntBuffer buffer = Buffers.newDirectIntBuffer(1);
        gl.glGenBuffers(1, buffer);
        GLUtils.checkError("glGenBuffers");
        this.bufferId = buffer.get();
    }

    public void dispose() {
        GL gl = GLUtils.getGL();

        if(bufferId != 0) {
            gl.glDeleteBuffers(1, new int[]{ bufferId }, 0);
            GLUtils.checkError("glDeleteBuffers");
        }
    }

    public void bind() {
        GL gl = GLUtils.getGL();

        gl.glBindBuffer(GL4.GL_UNIFORM_BUFFER, bufferId);
        GLUtils.checkError("glBindBuffer");
    }

    public void unbind() {
        GL gl = GLUtils.getGL();

        gl.glBindBuffer(GL4.GL_UNIFORM_BUFFER, 0);
        GLUtils.checkError("glBindBuffer");
    }

    public int getGLBufferId() {
        return bufferId;
    }

    public void setData(Buffer data) {
        GL gl = GLUtils.getGL();

        long size = data.capacity();
        if(data instanceof ShortBuffer) {
            size *= 2;
        }
        else if(data instanceof IntBuffer || data instanceof FloatBuffer) {
            size *= 4;
        }
        else if(data instanceof LongBuffer || data instanceof DoubleBuffer) {
            size *= 8;
        }

        bind();
        gl.glBufferData(GL4.GL_UNIFORM_BUFFER, size, data, GL.GL_DYNAMIC_DRAW);
        GLUtils.checkError("glBufferData");
        unbind();
    }
}
