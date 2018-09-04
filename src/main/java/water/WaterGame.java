package water;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import opengl.*;
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

    private GLShader shader;
    private GLVertexArray vertexArray;
    private GLVertexBuffer vertexBuffer;
    private GLUniformBuffer cameraUBO;

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
        //this.setLocation((1920 - width) / 2, (1080 - height) / 2);

        this.animator.start();
        this.canvas.requestFocus();
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Initialize");

        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLUtils.checkError("glClearColor");

        shader = new GLShader();
        shader.init(readFile("/shader/geometry.vs.glsl"), readFile("/shader/geometry.fs.glsl"));

        vertexBuffer = new GLVertexBuffer();
        vertexBuffer.setFormat(new DataFormat().add(DataFormat.DataType.FLOAT, 3));
        vertexBuffer.setData(Buffers.newDirectFloatBuffer(new float[]{
                0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.0f
        }));

        vertexArray = new GLVertexArray();
        vertexArray.addVertexBuffer(vertexBuffer);

        cameraUBO = new GLUniformBuffer();
        shader.addUniformBuffer(0, cameraUBO);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Dispose");

        if(vertexArray != null) {
            vertexArray.dispose();
        }
        if(vertexBuffer != null) {
            vertexBuffer.dispose();
        }
        if(shader != null) {
            shader.dispose();
        }
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        camera.input(keyDown);
        camera.update();

        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        GLUtils.checkError("glClear");

        FloatBuffer cameraBuffer = Buffers.newDirectFloatBuffer(16 * 2);
        camera.getProjMatrix().get(0, cameraBuffer);
        camera.getViewMatrix().get(16, cameraBuffer);
        cameraUBO.setData(cameraBuffer);

        shader.bind();
        vertexArray.bind();
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
        GLUtils.checkError("glDrawArrays");
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
