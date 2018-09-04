package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import java.nio.Buffer;
import java.nio.IntBuffer;

public class GLTexture implements GLObject {

    private int textureId;
    private int width;
    private int height;
    private boolean isDepth;
    private GLSampler sampler;

    public GLTexture() {
        GL gl = GLUtils.getGL();

        IntBuffer buffer = Buffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, buffer);
        GLUtils.checkError("glGenTextures");
        this.textureId = buffer.get();

        this.width = 0;
        this.height = 0;
        this.isDepth = false;
        this.sampler = null;
    }

    public void init(int width, int height, boolean isDepth, Buffer data) {
        GL gl = GLUtils.getGL();

        this.width = width;
        this.height = height;
        this.isDepth = isDepth;

        int iformat = (isDepth ? GL.GL_DEPTH_COMPONENT24 : GL.GL_RGBA);
        int format = (isDepth ? GL2.GL_DEPTH_COMPONENT : GL.GL_RGBA);
        int type = (isDepth ? GL.GL_FLOAT : GL.GL_UNSIGNED_BYTE);

        bind();
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, iformat, width, height, 0, format, type, data);
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

    public void setData(Buffer data) {
        GL gl = GLUtils.getGL();

        if(width > 0 && height > 0) {
            int format = (isDepth ? GL2.GL_DEPTH_COMPONENT : GL.GL_RGBA);
            int type = (isDepth ? GL.GL_FLOAT : GL.GL_UNSIGNED_BYTE);

            bind();
            gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, width, height, format, type, data);
            GLUtils.checkError("glTexSubImage2D");
            unbind();
        }
    }

    public void setSampler(GLSampler sampler) {
        this.sampler = sampler;
    }

    public GLSampler getSampler() {
        return sampler;
    }

    public void bind() {
        GL gl = GLUtils.getGL();

        gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
        GLUtils.checkError("glBindTexture");
    }

    public void unbind() {
        GL gl = GLUtils.getGL();

        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        GLUtils.checkError("glBindTexture");
    }

    public int getGLTextureId() {
        return textureId;
    }
}
