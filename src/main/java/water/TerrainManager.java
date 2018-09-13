package water;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import opengl.*;
import org.joml.Vector4f;
import utils.Camera;
import utils.DataFormat;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class TerrainManager {

    private GLManager manager;
    private int segments;

    private GLUniformBuffer cameraUBO;
    private GLUniformBuffer lightUBO;
    private GLTexture heightMap;
    private GLTexture normalMap;
    private GLTexture terrainTexture;

    private GLVertexArray vao;
    private GLIndexBuffer ibo;
    private GLShader shader;

    public TerrainManager(GLManager manager, int segments,
                        GLUniformBuffer cameraUBO, GLUniformBuffer lightUBO,
                        GLTexture heightMap, GLTexture normalMap, GLTexture terrainTexture) {
        this.manager = manager;
        this.segments = segments;
        this.cameraUBO = cameraUBO;
        this.lightUBO = lightUBO;
        this.heightMap = heightMap;
        this.normalMap = normalMap;
        this.terrainTexture = terrainTexture;
    }

    public void init(String vertexFile, String fragmentFile) {
        shader = manager.createShader(vertexFile, fragmentFile);
        shader.addUniformBuffer(0, cameraUBO);
        shader.addUniformBuffer(1, lightUBO);
        shader.addTexture("heightMap", heightMap);
        shader.addTexture("normalMap", normalMap);
        shader.addTexture("colorTexture", terrainTexture);

        vao = manager.createVertexArray();
        ibo = manager.createIndexBuffer();
        GLVertexBuffer vbo = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 3));

        FloatBuffer vertices = Buffers.newDirectFloatBuffer((segments + 1) * (segments + 1) * 3);
        for(int i=0; i <= segments; ++i) {
            float x = (float) i / (float) segments;
            for(int j=0; j <= segments; ++j) {
                float z = (float)j / (float) segments;
                vertices.put(x).put(0.0f).put(z);
            }
        }
        vertices.rewind();
        vbo.setData(vertices);
        vao.addVertexBuffer(vbo);

        IntBuffer indices = Buffers.newDirectIntBuffer(segments * segments * 6);
        for(int i=0; i<segments; ++i) {
            for(int j=0; j<segments; ++j) {
                indices.put(i + j * (segments + 1)).put(i + 1 + j * (segments + 1)).put(i + 1 + (j + 1) * (segments + 1));
                indices.put(i + 1 + (j + 1) * (segments + 1)).put(i + (j + 1) * (segments + 1)).put(i + j * (segments + 1));
            }
        }
        indices.rewind();
        ibo.setData(indices);
        ibo.setCount(segments * segments * 6);
    }

    public void render(Vector4f clippingPlane) {
        GL4 gl = GLUtils.getGL4();

        gl.glEnable(GL3.GL_CLIP_DISTANCE0);
        shader.bind();
        shader.setUniform4f("clippingPlane", clippingPlane);
        vao.bind();
        ibo.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, ibo.getCount(), GL.GL_UNSIGNED_INT, 0);
        GLUtils.checkError("glDrawArrays");
        ibo.unbind();
        vao.unbind();
        shader.unbind();
        gl.glDisable(GL3.GL_CLIP_DISTANCE0);
    }
}
