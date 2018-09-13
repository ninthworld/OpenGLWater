package water;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import opengl.*;
import utils.DataFormat;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class SkyboxManager {

    private GLManager manager;

    private GLUniformBuffer cameraUBO;
    private GLTextureCube skyboxTexture;

    private GLVertexArray vao;
    private GLIndexBuffer ibo;
    private GLShader shader;

    public SkyboxManager(GLManager manager,
                          GLUniformBuffer cameraUBO,
                          GLTextureCube skyboxTexture) {
        this.manager = manager;
        this.cameraUBO = cameraUBO;
        this.skyboxTexture = skyboxTexture;
    }

    public void init(String vertexFile, String fragmentFile) {
        shader = manager.createShader(vertexFile, fragmentFile);
        shader.addUniformBuffer(0, cameraUBO);
        shader.addTexture("skyboxTexture", skyboxTexture);

        vao = manager.createVertexArray();
        ibo = manager.createIndexBuffer();
        GLVertexBuffer vbo = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 3));

        FloatBuffer vertices = Buffers.newDirectFloatBuffer(new float[]{
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, -1.0f, 1.0f,
                1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, -1.0f,
                -1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, -1.0f
        });
        vbo.setData(vertices);
        vao.addVertexBuffer(vbo);

        IntBuffer indices = Buffers.newDirectIntBuffer(new int[]{
                0, 4, 1, 4, 5, 1,
                2, 3, 6, 3, 7, 6,
                0, 6, 4, 6, 0, 2,
                1, 5, 7, 7, 3, 1,
                0, 1, 3, 3, 2, 0,
                4, 7, 5, 7, 4, 6
        });
        ibo.setData(indices);
        ibo.setCount(36);
    }

    public void render() {
        GL4 gl = GLUtils.getGL4();

        gl.glDisable(GL.GL_DEPTH_TEST);
        shader.bind();
        vao.bind();
        ibo.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, ibo.getCount(), GL.GL_UNSIGNED_INT, 0);
        GLUtils.checkError("glDrawArrays");
        ibo.unbind();
        vao.unbind();
        shader.unbind();
        gl.glEnable(GL.GL_DEPTH_TEST);
    }
}
