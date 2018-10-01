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

public class WaterManager {

    private static final int SIZE = 256;
    private static final int OCTAVES = 4;

    private GLManager manager;
    private Camera camera;
    private int segments;
    private float time;

    private GLUniformBuffer cameraUBO;
    private GLUniformBuffer lightUBO;
    private GLFrameBuffer refractFBO;
    private GLFrameBuffer reflectFBO;

    private GLTexture heightMap;

    private GLVertexArray vao;
    private GLIndexBuffer ibo;
    private GLShader shader;

    private GLTexture[] noiseTextures = new GLTexture[OCTAVES];

    public WaterManager(GLManager manager, int segments, Camera camera,
                        GLUniformBuffer cameraUBO, GLUniformBuffer lightUBO,
                        GLFrameBuffer refractFBO, GLFrameBuffer reflectFBO,
                        GLTexture heightMap) {
        this.manager = manager;
        this.segments = segments;
        this.camera = camera;
        this.cameraUBO = cameraUBO;
        this.lightUBO = lightUBO;
        this.refractFBO = refractFBO;
        this.reflectFBO = reflectFBO;
        this.heightMap = heightMap;
        this.time = 0.0f;
    }

    public void init(String vertexFile, String fragmentFile) {
        shader = manager.createShader(vertexFile, fragmentFile);
        shader.addUniformBuffer(0, cameraUBO);
        shader.addUniformBuffer(1, lightUBO);
        shader.addTexture("refractTexture", refractFBO.getColorTexture(0));
        shader.addTexture("reflectTexture", reflectFBO.getColorTexture(0));
        shader.addTexture("heightMap", heightMap);

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

        // Generate Noise
        GLSampler sampler = manager.createSampler(GLSampler.EdgeType.WRAP, true);
        Noise[] perlin = new Noise[OCTAVES];
        for(int o=0; o<OCTAVES; ++o) {
            perlin[o] = new Noise();

            noiseTextures[o] = manager.createTexture(SIZE, SIZE, false, null);
            noiseTextures[o].setSampler(sampler);

            int pow = (int)Math.pow(2, o);

            ByteBuffer textureData = Buffers.newDirectByteBuffer(SIZE * SIZE * 4);
            for(int i=0; i<SIZE; ++i) {
                for(int j=0; j<SIZE; ++j) {
                    float normalHeight = perlin[o].noise(i * pow / 32.0f, j * pow / 32.0f, 8 * pow) * 0.5f + 0.5f;
                    byte val = (byte)(normalHeight * 256.0f);
                    textureData.put(val).put(val).put(val).put((byte)255);
                }
            }
            textureData.rewind();
            noiseTextures[o].setData(textureData);

            shader.addTexture("noiseTextures[" + o + "]", noiseTextures[o]);
        }
    }

    public void render() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glDisable(GL.GL_CULL_FACE);

        shader.bind();

        shader.setUniform3f("cameraPos", camera.getPosition());
        shader.setUniform1f("time", time += 0.01f);

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
