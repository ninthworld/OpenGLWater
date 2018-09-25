package noise;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import opengl.*;
import org.joml.SimplexNoise;
import org.joml.Vector3f;
import utils.Camera;
import utils.DataFormat;

import javax.swing.*;
import java.nio.ByteBuffer;

public class NoiseGame extends JFrame implements GLEventListener {

    private int width;
    private int height;

    private GLCanvas canvas;
    private Animator animator;

    private GLManager manager;

    private GLTexture noiseTexture;
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

    private static final int SIZE = 256;

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

        // Texture
        noiseTexture = manager.createTexture(SIZE, SIZE, false, null);
        noiseTexture.setSampler(sampler);

        // Shader
        quadShader = manager.createShader("/shader/quad.vs.glsl", "/shader/quad.fs.glsl");
        quadShader.addTexture("colorTexture", noiseTexture);

        // Generate Noise
        SimplexNoise noise = new SimplexNoise();

        ByteBuffer textureData = Buffers.newDirectByteBuffer(SIZE * SIZE * 4);
        for(int i=0; i<SIZE; ++i) {
            for(int j=0; j<SIZE; ++j) {
                float normalHeight = tiledNoise(i, j, SIZE, noise);
                byte val = (byte)(normalHeight * 256.0f);
                textureData.put(val).put(val).put(val).put((byte)255);
            }
        }
        textureData.rewind();

        noiseTexture.setData(textureData);
    }

    // Tiled 2D Simplex Noise from 4D Noise Space
    // by Ron Valstar <http://ronvalstar.nl/creating-tileable-noise-maps>
    private float tiledNoise(float x, float y, int iSize, SimplexNoise s) {
        final float fNoiseScale = 0.8f;
        final float fRds = 1.2f;
        float fNX = x / iSize;
        float fNY = y / iSize;
        float fRdx = fNX * 2.0f * (float)Math.PI;
        float fRdy = fNY * 2.0f * (float)Math.PI;
        float a = fRds * (float)Math.sin(fRdx);
        float b = fRds * (float)Math.cos(fRdx);
        float c = fRds * (float)Math.sin(fRdy);
        float d = fRds * (float)Math.cos(fRdy);
        float v = s.noise(123 + a * fNoiseScale, 231 + b * fNoiseScale, 312 + c * fNoiseScale, 273 + d * fNoiseScale);
        return Math.min(Math.max(v / 2.0f + 0.5f, 0.0f), 1.0f);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Dispose");

        manager.dispose();
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glViewport(0, 0, SIZE * 2, SIZE * 2);

        quadVAO.bind();
        quadIBO.bind();

        quadShader.bind();
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
