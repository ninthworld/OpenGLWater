package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class GLFrameBuffer {

    private int bufferId;
    private Map<Integer, GLTexture> colorTextures;
    private GLTexture depthTexture;

    public GLFrameBuffer() {
        GL gl = GLUtils.getGL();

        IntBuffer buffer = Buffers.newDirectIntBuffer(1);
        gl.glGenFramebuffers(1, buffer);
        GLUtils.checkError("glGenFramebuffers");
        this.bufferId = buffer.get();

        this.colorTextures = new HashMap<>();
        this.depthTexture = null;
    }

    public void dispose() {
        GL gl = GLUtils.getGL();

        if(bufferId != 0) {
            gl.glDeleteFramebuffers(1, new int[]{ bufferId }, 0);
            GLUtils.checkError("glDeleteFramebuffers");
        }
    }

    public void bind() {
        GL gl = GLUtils.getGL();

        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, bufferId);
        GLUtils.checkError("glBindFramebuffer");
    }

    public void unbind() {
        GL gl = GLUtils.getGL();

        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        GLUtils.checkError("glBindFramebuffer");
    }

    public void addColorTexture(int binding, GLTexture texture) {
        GL2 gl = GLUtils.getGL2();

        colorTextures.put(binding, texture);

        bind();
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0 + binding, GL.GL_TEXTURE_2D, texture.getGLTextureId(), 0);
        GLUtils.checkError("glFramebufferTexture2D");

        IntBuffer colorAttachments = Buffers.newDirectIntBuffer(colorTextures.size());
        for(Integer key : colorTextures.keySet()) {
            colorAttachments.put(GL.GL_COLOR_ATTACHMENT0 + key);
        }
        colorAttachments.rewind();
        gl.glDrawBuffers(colorTextures.size(), colorAttachments);

        GLUtils.checkFramebufferError();
        unbind();
    }

    public void setDepthTexture(GLTexture texture) {
        GL2 gl = GLUtils.getGL2();

        depthTexture = texture;

        bind();
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_TEXTURE_2D, texture.getGLTextureId(), 0);
        GLUtils.checkError("glFramebufferTexture2D");
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_TEXTURE_2D, texture.getGLTextureId(), 0);
        GLUtils.checkError("glFramebufferTexture2D");

        GLUtils.checkFramebufferError();
        unbind();
    }

    public GLTexture getColorTexture(int binding) {
        return colorTextures.get(binding);
    }

    public GLTexture getDepthTexture() {
        return depthTexture;
    }
}
