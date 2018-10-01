package noise;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import opengl.*;
import org.joml.SimplexNoise;
import org.joml.Vector2f;
import org.joml.Vector3f;
import utils.Camera;
import utils.DataFormat;

import javax.swing.*;
import java.nio.ByteBuffer;

public class NoiseGame extends JFrame implements GLEventListener {

    private static final int SIZE = 256;
    private static final int OCTAVES = 4;

    private int width;
    private int height;

    private GLCanvas canvas;
    private Animator animator;

    private GLManager manager;

    private GLTexture[] noiseTextures = new GLTexture[OCTAVES];

    private GLVertexArray quadVAO;
    private GLIndexBuffer quadIBO;
    private GLShader quadShader;

    private Camera camera;

    public NoiseGame(String title, int width, int height) {
        super(title);

        this.width = width;
        this.height = height;

        this.canvas = new GLCanvas();
        this.animator = new Animator(this.canvas);
        this.manager = new GLManager();

        this.camera = new Camera(width, height);
        this.camera.setPosition(new Vector3f(0.0f, 10.0f, 0.0f));

        this.add(this.canvas);
        this.canvas.addGLEventListener(this);
        this.canvas.setFocusable(true);

        this.setSize(width, height);
        this.setVisible(true);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.animator.start();
        this.canvas.requestFocus();
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Initialize");

        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLUtils.checkError("glClearColor");

        // Common
        GLSampler sampler = manager.createSampler(GLSampler.EdgeType.WRAP, false);

        // Geometry
        quadVAO = manager.createVertexArray();
        GLVertexBuffer quadVBO = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 2));
        quadVBO.setData(Buffers.newDirectFloatBuffer(new float[]{
                -1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        }));
        quadVAO.addVertexBuffer(quadVBO);

        quadIBO = manager.createIndexBuffer();
        quadIBO.setData(Buffers.newDirectIntBuffer(new int[]{
                0, 1, 2, 2, 1, 3
        }));
        quadIBO.setCount(6);

        // Shader
        quadShader = manager.createShader("/shader/quad.vs.glsl", "/shader/noise.fs.glsl");

        // Generate Noise
        Noise perlin = new Noise();
        for(int o=0; o<OCTAVES; ++o) {
            noiseTextures[o] = manager.createTexture(SIZE, SIZE, false, null);
            noiseTextures[o].setSampler(sampler);

            int pow = (int)Math.pow(2, o);

            ByteBuffer textureData = Buffers.newDirectByteBuffer(SIZE * SIZE * 4);
            for(int i=0; i<SIZE; ++i) {
                for(int j=0; j<SIZE; ++j) {
                    float normalHeight = perlin.noise(i * pow / 32.0f, j * pow / 32.0f, 8 * pow) * 0.5f + 0.5f;
                    byte val = (byte)(normalHeight * 256.0f);
                    textureData.put(val).put(val).put(val).put((byte)255);
                }
            }
            textureData.rewind();
            noiseTextures[o].setData(textureData);

            quadShader.addTexture("colorTextures[" + o + "]", noiseTextures[o]);
        }
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Dispose");

        manager.dispose();
    }

    private float time = 0.0f;

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glViewport(0, 0, SIZE * 2, SIZE * 2);

        quadVAO.bind();
        quadIBO.bind();

        quadShader.bind();

        quadShader.setUniform1f("time", time += 0.01f);

        gl.glDrawElements(GL.GL_TRIANGLES, quadIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        quadShader.unbind();

        quadIBO.unbind();
        quadVAO.unbind();
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h) {
    }

    public static void main(String[] args) {
        new NoiseGame("OpenGL Water", 1280, 800);
    }
}
