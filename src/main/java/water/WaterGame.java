package water;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import opengl.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import utils.Camera;
import utils.DataFormat;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

public class WaterGame extends JFrame implements GLEventListener, KeyListener {

    private GLCanvas canvas;
    private Animator animator;
    private boolean[] keyDown;

    private GLShader geometryShader;
    private GLShader fxShader;
    private GLVertexArray quadVAO;
    private GLVertexBuffer quadVBO;
    private GLVertexArray geometryVAO;
    private GLVertexBuffer geometryVBO;
    private GLUniformBuffer cameraUBO;
    private GLUniformBuffer invCameraUBO;

    private GLSampler sampler;
    private GLTexture colorTexture;
    private GLTexture depthTexture;
    private GLFrameBuffer frameBuffer;

    private Camera camera;

    public WaterGame(String title, int width, int height) {
        super(title);

        this.canvas = new GLCanvas();
        this.animator = new Animator(this.canvas);
        this.keyDown = new boolean[256];

        this.camera = new Camera(width, height);
        this.camera.setPosition(new Vector3f(0.0f, 0.0f, -2.0f));

        this.add(this.canvas);
        this.canvas.addGLEventListener(this);
        this.canvas.setFocusable(true);
        this.canvas.addKeyListener(this);

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

        // Geometry
        geometryShader = new GLShader();
        geometryShader.init(readFile("/shader/geometry.vs.glsl"), readFile("/shader/geometry.fs.glsl"));

        geometryVBO = new GLVertexBuffer();
        geometryVBO.setFormat(new DataFormat().add(DataFormat.DataType.FLOAT, 3));
        geometryVBO.setData(Buffers.newDirectFloatBuffer(new float[]{
                0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.0f
        }));

        geometryVAO = new GLVertexArray();
        geometryVAO.addVertexBuffer(geometryVBO);

        cameraUBO = new GLUniformBuffer();
        geometryShader.addUniformBuffer(0, cameraUBO);

        // FX
        fxShader = new GLShader();
        fxShader.init(readFile("/shader/fx.vs.glsl"), readFile("/shader/fx.fs.glsl"));

        quadVBO = new GLVertexBuffer();
        quadVBO.setFormat(new DataFormat().add(DataFormat.DataType.FLOAT, 2));
        quadVBO.setData(Buffers.newDirectFloatBuffer(new float[]{
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f
        }));

        quadVAO = new GLVertexArray();
        quadVAO.addVertexBuffer(quadVBO);

        sampler = new GLSampler();
        sampler.init(GLSampler.EdgeType.WRAP, true);

        colorTexture = new GLTexture();
        colorTexture.init(1280, 800, false, null);
        colorTexture.setSampler(sampler);

        depthTexture = new GLTexture();
        depthTexture.init(1280, 800, true, null);
        depthTexture.setSampler(sampler);

        frameBuffer = new GLFrameBuffer();
        frameBuffer.addColorTexture(0, colorTexture);
        frameBuffer.setDepthTexture(depthTexture);

        invCameraUBO = new GLUniformBuffer();
        fxShader.addUniformBuffer(0, invCameraUBO);

        fxShader.addTexture(0, colorTexture);
        fxShader.addTexture(1, depthTexture);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Dispose");

        if(sampler != null) {
            sampler.dispose();
        }
        if(colorTexture != null) {
            colorTexture.dispose();
        }
        if(depthTexture != null) {
            depthTexture.dispose();
        }
        if(frameBuffer != null) {
            frameBuffer.dispose();
        }
        if(geometryVAO != null) {
            geometryVAO.dispose();
        }
        if(geometryVBO != null) {
            geometryVBO.dispose();
        }
        if(quadVAO != null) {
            quadVAO.dispose();
        }
        if(quadVBO != null) {
            quadVBO.dispose();
        }
        if(cameraUBO != null) {
            cameraUBO.dispose();
        }
        if(invCameraUBO != null) {
            invCameraUBO.dispose();
        }
        if(geometryShader != null) {
            geometryShader.dispose();
        }
        if(fxShader != null) {
            fxShader.dispose();
        }
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        camera.input(keyDown);
        camera.update();

        GL4 gl = (GL4) GLContext.getCurrentGL();

        FloatBuffer cameraBuffer = Buffers.newDirectFloatBuffer(16 * 2);
        camera.getProjMatrix().get(0, cameraBuffer);
        camera.getViewMatrix().get(16, cameraBuffer);
        cameraUBO.setData(cameraBuffer);

        FloatBuffer invCameraBuffer = Buffers.newDirectFloatBuffer(16 * 2);
        camera.getProjMatrix().invertPerspective(new Matrix4f()).get(0, invCameraBuffer);
        camera.getViewMatrix().invert(new Matrix4f()).get(16, invCameraBuffer);
        invCameraUBO.setData(invCameraBuffer);

        frameBuffer.bind();
        gl.glViewport(0, 0, 1280,800);

        gl.glEnable(GL.GL_DEPTH_TEST);

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        GLUtils.checkError("glClear");

        geometryShader.bind();
        geometryVAO.bind();

        gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
        GLUtils.checkError("glDrawArrays");

        geometryVAO.unbind();
        geometryShader.unbind();

        gl.glDisable(GL.GL_DEPTH_TEST);

        frameBuffer.unbind();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        GLUtils.checkError("glClear");

        fxShader.bind();
        quadVAO.bind();

        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        GLUtils.checkError("glDrawArrays");

        quadVAO.unbind();
        fxShader.unbind();
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h) {
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if(key < keyDown.length) {
            keyDown[key] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if(key < keyDown.length) {
            keyDown[key] = false;
        }
    }

    private String readFile(String file) {
        StringBuilder builder = new StringBuilder();
        try {
            InputStream in = new BufferedInputStream(getClass().getResourceAsStream(file));
            while (in.available() > 0) {
                builder.append((char) in.read());
            }
            in.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public static void main(String[] args) {
        new WaterGame("OpenGL Water", 1280, 800);
    }
}
