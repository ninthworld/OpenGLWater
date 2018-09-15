package fft;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import opengl.*;
import org.joml.Random;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import utils.DataFormat;

import javax.swing.*;
import java.nio.FloatBuffer;

public class FFTGame extends JFrame implements GLEventListener {

    private int width;
    private int height;

    private GLCanvas canvas;
    private Animator animator;
    private GLManager manager;

    private GLVertexArray quadVAO;
    private GLIndexBuffer quadIBO;

    private GLShader initialSpectrumShader;
    private GLShader phaseShader;
    private GLShader spectrumShader;
    private GLShader subtransformShader;
    private GLShader normalShader;

    private GLShader simpleShader;

    private GLFrameBuffer[] phasesFBO;
    private GLFrameBuffer[] phasesTransformFBO;
    private GLFrameBuffer spectrumFBO;
    private GLFrameBuffer displacementFBO;
    private GLFrameBuffer normalFBO;

    public FFTGame(String title, int width, int height) {
        super(title);

        this.width = width;
        this.height = height;

        this.canvas = new GLCanvas();
        this.animator = new Animator(this.canvas);
        this.manager = new GLManager();

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

    final Vector2fc wind = new Vector2f(10.0f, 10.0f);
    final float choppiness = 1.0f;
    final int resolution = 512;
    final int size = 250;

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Initialize");

        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLUtils.checkError("glClearColor");

        // Sampler
        GLSampler samplerNearest = manager.createSampler(GLSampler.EdgeType.CLAMP, false);
        GLSampler samplerLinear = manager.createSampler(GLSampler.EdgeType.CLAMP, true);

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

        // Initial Spectrum
        GLTexture initialSpectrumTexture = manager.createTexture32f(resolution, resolution, null);
        initialSpectrumTexture.setSampler(samplerNearest);
        GLFrameBuffer initialSpectrumFBO = manager.createFrameBuffer();
        initialSpectrumFBO.addColorTexture(0, initialSpectrumTexture);

        initialSpectrumShader = manager.createShader("/shader/fft/quad.vs.glsl", "/shader/fft/initial_spectrum.fs.glsl");
        initialSpectrumFBO.bind();
        gl.glViewport(0, 0, resolution, resolution);
        initialSpectrumShader.bind();
        initialSpectrumShader.setUniform2f("wind", wind);
        initialSpectrumShader.setUniform1f("resolution", resolution);
        initialSpectrumShader.setUniform1f("size", size);
        quadVAO.bind();
        quadIBO.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, quadIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        quadIBO.unbind();
        quadVAO.unbind();
        initialSpectrumShader.unbind();
        initialSpectrumFBO.unbind();

        // Phase
        phaseShader = manager.createShader("/shader/fft/quad.vs.glsl", "/shader/fft/phase.fs.glsl");
        phaseShader.bind();
        phaseShader.setUniform1f("resolution", resolution);
        phaseShader.setUniform1f("size", size);
        phaseShader.unbind();

        // Spectrum
        spectrumShader = manager.createShader("/shader/fft/quad.vs.glsl", "/shader/fft/spectrum.fs.glsl");
        spectrumShader.addTexture("initialSpectrum", initialSpectrumTexture);
        spectrumShader.bind();
        spectrumShader.setUniform1f("choppiness", choppiness);
        spectrumShader.setUniform1f("resolution", resolution);
        spectrumShader.setUniform1f("size", size);
        spectrumShader.unbind();

        GLTexture spectrumTexture = manager.createTexture32f(resolution, resolution, null);
        spectrumTexture.setSampler(samplerNearest);
        spectrumFBO = manager.createFrameBuffer();
        spectrumFBO.addColorTexture(0, spectrumTexture);

        // Swap phase mechanism
        Random rand = new Random(12345);
        FloatBuffer phaseBuffer = Buffers.newDirectFloatBuffer(resolution * resolution * 4);
        for(int i=0; i<resolution; ++i) {
            for(int j=0; j<resolution; ++j) {
                phaseBuffer.put(rand.nextFloat() * 2.0f * (float)Math.PI).put(0.0f).put(0.0f).put(0.0f);
            }
        }
        phaseBuffer.rewind();

        GLTexture aPhase = manager.createTexture32f(resolution, resolution, null);
        aPhase.setSampler(samplerNearest);
        GLTexture bPhase = manager.createTexture32f(resolution, resolution, phaseBuffer);
        bPhase.setSampler(samplerNearest);

        phasesFBO = new GLFrameBuffer[2];
        phasesFBO[0] = manager.createFrameBuffer();
        phasesFBO[0].addColorTexture(0, aPhase);
        phasesFBO[1] = manager.createFrameBuffer();
        phasesFBO[1].addColorTexture(0, bPhase);

        GLTexture aTransPhase = manager.createTexture32f(resolution, resolution, null);
        aPhase.setSampler(samplerNearest);
        GLTexture bTransPhase = manager.createTexture32f(resolution, resolution, phaseBuffer);
        bPhase.setSampler(samplerNearest);

        phasesTransformFBO = new GLFrameBuffer[2];
        phasesTransformFBO[0] = manager.createFrameBuffer();
        phasesTransformFBO[0].addColorTexture(0, aTransPhase);
        phasesTransformFBO[1] = manager.createFrameBuffer();
        phasesTransformFBO[1].addColorTexture(0, bTransPhase);

        // Subtransform
        subtransformShader = manager.createShader("/shader/fft/quad.vs.glsl", "/shader/fft/subtransform.fs.glsl");
        subtransformShader.bind();
        subtransformShader.setUniform1f("resolution", resolution);
        subtransformShader.unbind();

        // Displacement
        GLTexture displacementTexture = manager.createTexture32f(resolution, resolution, null);
        displacementTexture.setSampler(samplerLinear);
        displacementFBO = manager.createFrameBuffer();
        displacementFBO.addColorTexture(0, displacementTexture);

        // Normal
        GLTexture normalTexture = manager.createTexture32f(resolution, resolution, null);
        normalTexture.setSampler(samplerLinear);
        normalFBO = manager.createFrameBuffer();
        normalFBO.addColorTexture(0, normalTexture);

        normalShader = manager.createShader("/shader/fft/quad.vs.glsl", "/shader/fft/normal.fs.glsl");
        normalShader.addTexture("displacementMap", displacementTexture);
        normalShader.bind();
        normalShader.setUniform1f("resolution", resolution);
        normalShader.setUniform1f("size", size);
        normalShader.unbind();

        // Straight-to-screen
        simpleShader = manager.createShader("/shader/fft/quad.vs.glsl", "/shader/fft/simple.fs.glsl");
        simpleShader.addTexture("colorTexture", spectrumFBO.getColorTexture(0));
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Dispose");

        manager.dispose();
    }

    float deltaTime = 0.0001f;
    int phaseIndex = 0;

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glViewport(0, 0, resolution, resolution);

        quadVAO.bind();
        quadIBO.bind();

        int nextPhaseIndex = (phaseIndex + 1) % 2;

        phasesFBO[phaseIndex].bind();

        phaseShader.addTexture("phases", phasesFBO[nextPhaseIndex].getColorTexture(0));
        phaseShader.bind();
        phaseShader.setUniform1f("deltaTime", deltaTime);
        gl.glDrawElements(GL.GL_TRIANGLES, quadIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        phaseShader.unbind();

        spectrumFBO.bind();

        phaseShader.addTexture("phases", phasesFBO[phaseIndex].getColorTexture(0));
        phaseShader.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, quadIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        phaseShader.unbind();

        phaseIndex = nextPhaseIndex;

        subtransformShader.bind();
        subtransformShader.setUniform1f("direction", 0.0f);
        int iterations = (int)(Math.log(resolution) / Math.log(2)) * 2;
        for(int i=0; i<iterations; ++i) {
            if(i == 0) {
                phasesTransformFBO[0].bind();
                subtransformShader.addTexture("inputTexture", spectrumFBO.getColorTexture(0));
                subtransformShader.updateTextures();
            }
            else if(i == iterations - 1) {
                displacementFBO.bind();
                subtransformShader.addTexture("inputTexture", phasesTransformFBO[(iterations % 2)].getColorTexture(0));
                subtransformShader.updateTextures();
            }
            else if(i % 2 == 1) {
                phasesTransformFBO[1].bind();
                subtransformShader.addTexture("inputTexture", phasesTransformFBO[0].getColorTexture(0));
                subtransformShader.updateTextures();
            }
            else {
                phasesTransformFBO[0].bind();
                subtransformShader.addTexture("inputTexture", phasesTransformFBO[1].getColorTexture(0));
                subtransformShader.updateTextures();
            }

            if(i == iterations / 2) {
                subtransformShader.setUniform1f("direction", 1.0f);
            }

            subtransformShader.setUniform1f("subtransformSize", (float)Math.pow(2, (i % (iterations / 2)) + 1.0f));
            gl.glDrawElements(GL.GL_TRIANGLES, quadIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        }
        subtransformShader.unbind();

        normalFBO.bind();
        normalShader.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, quadIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        normalFBO.unbind();

        manager.clear();

        simpleShader.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, quadIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        simpleShader.unbind();

        quadIBO.unbind();
        quadVAO.unbind();
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h) {
    }

    public static void main(String[] args) {
        new FFTGame("FFT Game", 1280, 800);
    }
}
