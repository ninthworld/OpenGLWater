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
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class WaterGame extends JFrame implements GLEventListener, KeyListener {

    private GLCanvas canvas;
    private Animator animator;
    private boolean[] keyDown;

    private GLManager manager;
    private GLShader geometryShader;
    private GLShader fxShader;
    private GLVertexArray quadVAO;
    private GLVertexArray geometryVAO;
    private GLIndexBuffer geometryIBO;
    private GLUniformBuffer cameraUBO;
    private GLUniformBuffer invCameraUBO;
    private GLFrameBuffer frameBuffer;

    private Camera camera;

    public WaterGame(String title, int width, int height) {
        super(title);

        this.canvas = new GLCanvas();
        this.animator = new Animator(this.canvas);
        this.keyDown = new boolean[256];
        this.manager = new GLManager();

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

        // Common
        GLSampler sampler = manager.createSampler(GLSampler.EdgeType.WRAP, true);

        // Geometry
        geometryShader = manager.createShader(readFile("/shader/geometry.vs.glsl"), readFile("/shader/geometry.fs.glsl"));

        GLVertexBuffer geometryVBO = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 3));
        geometryVBO.setData(Buffers.newDirectFloatBuffer(new float[]{
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, -1.0f, 1.0f,
                1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, -1.0f,
                -1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, -1.0f
        }));

        geometryIBO = manager.createIndexBuffer();
        geometryIBO.setData(Buffers.newDirectIntBuffer(new int[]{
                0, 1, 4, 4, 1, 5,
                2, 6, 3, 3, 6, 7,
                0, 4, 6, 6, 0, 2,
                1, 5, 7, 7, 1, 3,
                0, 1, 3, 3, 0, 2,
                4, 5, 7, 7, 4, 6
        }));

        geometryVAO = manager.createVertexArray();
        geometryVAO.addVertexBuffer(geometryVBO);

        cameraUBO = manager.createUniformBuffer();
        geometryShader.addUniformBuffer(0, cameraUBO);

        // FX
        fxShader = manager.createShader(readFile("/shader/fx.vs.glsl"), readFile("/shader/fx.fs.glsl"));

        GLVertexBuffer quadVBO = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 2));
        quadVBO.setData(Buffers.newDirectFloatBuffer(new float[]{
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f
        }));

        quadVAO = manager.createVertexArray();
        quadVAO.addVertexBuffer(quadVBO);

        GLTexture colorTexture = manager.createTexture(1280, 800, false, null);
        colorTexture.setSampler(sampler);
        fxShader.addTexture(0, colorTexture);

        GLTexture depthTexture = manager.createTexture(1280, 800, true, null);
        depthTexture.setSampler(sampler);
        fxShader.addTexture(1, depthTexture);

        frameBuffer = manager.createFrameBuffer();
        frameBuffer.addColorTexture(0, colorTexture);
        frameBuffer.setDepthTexture(depthTexture);

        invCameraUBO = manager.createUniformBuffer();
        fxShader.addUniformBuffer(0, invCameraUBO);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Dispose");

        manager.dispose();
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

        geometryIBO.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, 6 * 6, GL.GL_UNSIGNED_INT, 0);
        GLUtils.checkError("glDrawArrays");
        geometryIBO.unbind();

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
        if(key == VK_ESCAPE) {
            this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
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
