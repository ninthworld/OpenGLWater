package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import java.nio.Buffer;
import java.nio.IntBuffer;

public class GLTextureCube extends GLTexture implements GLObject {

    private int textureId;
    private int width;
    private int height;
    private GLSampler sampler;

    public GLTextureCube() {
        GL gl = GLUtils.getGL();

        IntBuffer buffer = Buffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, buffer);
        GLUtils.checkError("glGenTextures");
        this.textureId = buffer.get();

        this.width = 0;
        this.height = 0;
        this.sampler = null;
    }

    @Override
    public void init(int width, int height, boolean isDepth, Buffer data) {
    }

    public void init(int width, int height, Buffer[] data) {
        GL gl = GLUtils.getGL();

        this.width = width;
        this.height = height;

        int iformat = GL.GL_RGBA;
        int format = GL.GL_RGBA;
        int type = GL.GL_UNSIGNED_BYTE;

        bind();
        for(int i=0; i<data.length; ++i) {
            gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, iformat, width, height, 0, format, type, data[i]);
        }
        unbind();
    }

    @Override
    public void dispose() {
        GL gl = GLUtils.getGL();

        if(textureId != 0) {
            gl.glDeleteTextures(1, new int[]{ textureId }, 0);
            GLUtils.checkError("glDeleteTextures");
        }
    }

    @Override
    public void setData(Buffer data) {
    }

    public void setData(Buffer[] data) {
        GL gl = GLUtils.getGL();

        if(width > 0 && height > 0) {
            int format = GL.GL_RGBA;
            int type = GL.GL_UNSIGNED_BYTE;

            bind();
            for(int i=0; i<data.length; ++i) {
                gl.glTexSubImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, 0, 0, width, height, format, type, data[i]);
                GLUtils.checkError("glTexSubImage2D");
            }
            unbind();
        }
    }

    @Override
    public void bind() {
        GL gl = GLUtils.getGL();

        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, textureId);
        GLUtils.checkError("glBindTexture");
    }

    @Override
    public void unbind() {
        GL gl = GLUtils.getGL();

        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, 0);
        GLUtils.checkError("glBindTexture");
    }
}
