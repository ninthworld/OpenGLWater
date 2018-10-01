package water;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;
import noise.Noise;
import opengl.*;
import org.joml.Vector4f;
import utils.Camera;
import utils.DataFormat;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class UnderwaterManager {

    private GLManager manager;
    private Camera camera;

    private GLFrameBuffer sceneFBO;

    private GLUniformBuffer invCameraUBO;

    private GLVertexArray vao;
    private GLIndexBuffer ibo;
    private GLShader shader;

    public UnderwaterManager(GLManager manager, Camera camera, GLUniformBuffer invCameraUBO, GLFrameBuffer sceneFBO) {
        this.manager = manager;
        this.sceneFBO = sceneFBO;
        this.camera = camera;
        this.invCameraUBO = invCameraUBO;
    }

    public void init(String vertexFile, String fragmentFile) {
        shader = manager.createShader(vertexFile, fragmentFile);
        shader.addUniformBuffer(0, invCameraUBO);
        shader.addTexture("colorTexture", sceneFBO.getColorTexture(0));
        shader.addTexture("depthTexture", sceneFBO.getDepthTexture());

        vao = manager.createVertexArray();
        GLVertexBuffer quadVBO = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 2));
        quadVBO.setData(Buffers.newDirectFloatBuffer(new float[]{
                -1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        }));
        vao.addVertexBuffer(quadVBO);

        ibo = manager.createIndexBuffer();
        ibo.setData(Buffers.newDirectIntBuffer(new int[]{
                0, 1, 2, 2, 1, 3
        }));
        ibo.setCount(6);
    }

    public void render() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glDisable(GL.GL_CULL_FACE);

        shader.bind();

        shader.setUniform3f("cameraPos", camera.getPosition());

        vao.bind();
        ibo.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, ibo.getCount(), GL.GL_UNSIGNED_INT, 0);
        GLUtils.checkError("glDrawArrays");
        ibo.unbind();
        vao.unbind();
        shader.unbind();

        gl.glEnable(GL.GL_CULL_FACE);
    }
}
