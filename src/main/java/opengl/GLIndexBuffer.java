package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import utils.DataFormat;

import java.nio.*;

public class GLIndexBuffer implements GLObject {

    private int bufferId;
    private int count;

    public GLIndexBuffer() {
        GL gl = GLUtils.getGL();

        IntBuffer buffer = Buffers.newDirectIntBuffer(1);
        gl.glGenBuffers(1, buffer);
        GLUtils.checkError("glGenBuffers");
        this.bufferId = buffer.get();
        this.count = 0;
    }

    @Override
    public void dispose() {
        GL gl = GLUtils.getGL();

        if(bufferId != 0) {
            gl.glDeleteBuffers(1, new int[]{ bufferId }, 0);
            GLUtils.checkError("glDeleteBuffers");
        }
    }

    public void bind() {
        GL gl = GLUtils.getGL();

        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, bufferId);
        GLUtils.checkError("glBindBuffer");
    }

    public void unbind() {
        GL gl = GLUtils.getGL();

        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLUtils.checkError("glBindBuffer");
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
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, size, data, GL.GL_STATIC_DRAW);
        GLUtils.checkError("glBufferData");
        unbind();
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
