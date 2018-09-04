package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;

import java.nio.IntBuffer;

public class GLSampler implements GLObject {

    private int samplerId;

    public GLSampler() {
        GL3 gl = GLUtils.getGL3();

        IntBuffer buffer = Buffers.newDirectIntBuffer(1);
        gl.glGenSamplers(1, buffer);
        GLUtils.checkError("glGenSamplers");
        this.samplerId = buffer.get();
    }

    public void init(EdgeType edge, boolean linear) {
        GL3 gl = GLUtils.getGL3();

        int edgeType = GL.GL_REPEAT;
        switch (edge) {
            case MIRROR: edgeType = GL.GL_MIRRORED_REPEAT; break;
            case CLAMP: edgeType = GL.GL_CLAMP_TO_EDGE; break;
            case BORDER: edgeType = GL2.GL_CLAMP_TO_BORDER; break;
        }

        int filterType = (linear ? GL.GL_LINEAR : GL.GL_NEAREST);

        gl.glSamplerParameteri(samplerId, GL.GL_TEXTURE_WRAP_S, edgeType);
        GLUtils.checkError("glSamplerParameteri");
        gl.glSamplerParameteri(samplerId, GL.GL_TEXTURE_WRAP_T, edgeType);
        GLUtils.checkError("glSamplerParameteri");
        gl.glSamplerParameteri(samplerId, GL2.GL_TEXTURE_WRAP_R, edgeType);
        GLUtils.checkError("glSamplerParameteri");

        gl.glSamplerParameteri(samplerId, GL.GL_TEXTURE_MIN_FILTER, filterType);
        GLUtils.checkError("glSamplerParameteri");
        gl.glSamplerParameteri(samplerId, GL.GL_TEXTURE_MAG_FILTER, filterType);
        GLUtils.checkError("glSamplerParameteri");
    }

    @Override
    public void dispose() {
        GL3 gl = GLUtils.getGL3();

        if(samplerId != 0) {
            gl.glDeleteSamplers(1, new int[]{ samplerId }, 0);
            GLUtils.checkError("glDeleteSamplers");
        }
    }

    public int getGLSamplerId() {
        return samplerId;
    }

    public enum EdgeType {
        MIRROR, CLAMP, BORDER, WRAP
    }
}
