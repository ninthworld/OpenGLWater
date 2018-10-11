package tutorial0;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import graphicslib3D.MatrixStack;
import graphicslib3D.Point3D;
import graphicslib3D.Vector3D;
import graphicslib3D.Vertex3D;
import graphicslib3D.shape.Sphere;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class Starter extends JFrame implements GLEventListener, KeyListener {

    private GLCanvas canvas;
    private FPSAnimator animator;
    private boolean[] keyDown;

    private Camera camera;

    private int skyboxTextureId;
    private int gridTextureId;

    private int skyboxVAO;

    private int groundVAO;

    private int sphereVAO;
    private int sphereIBO;
    private int sphereIndexCount;
    private Point3D spherePosition;

    private int shapeShaderId;
    private int shapeProjMatrixU;
    private int shapeModelViewMatrixU;

    private Vector3D lightDirection;

    public Starter() {
        super("Tutorial 0 - A Simple Scene");

        this.keyDown = new boolean[256];
        this.canvas = new GLCanvas();
        this.canvas.addGLEventListener(this);
        this.canvas.setFocusable(true);
        this.canvas.addKeyListener(this);
        this.add(this.canvas);

        this.setSize(1280, 960);
        this.setVisible(true);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.animator = new FPSAnimator(this.canvas, 60);
        this.animator.start();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        // Global Settings
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Initialize Light
        lightDirection = new Vector3D(0.0, 0.5, 1.0).normalize();

        // Initialize Camera
        camera = new Camera(45.0f, (float) this.getWidth() / (float) this.getHeight(), 0.1f, 1000.0f);
        camera.move(-1.0f, camera.getForward());

        // Load Skybox Texture


        // Load Grid Texture


        // Load Shape Shader
        shapeShaderId = gl.glCreateProgram();

        int shaderId;
        shaderId = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        gl.glShaderSource(shaderId, 1, new String[]{ readFile("/tutorial0/shape.vs.glsl") }, null);
        gl.glCompileShader(shaderId);
        gl.glAttachShader(shapeShaderId, shaderId);

        shaderId = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        gl.glShaderSource(shaderId, 1, new String[]{ readFile("/tutorial0/shape.fs.glsl") }, null);
        gl.glCompileShader(shaderId);
        gl.glAttachShader(shapeShaderId, shaderId);

        gl.glLinkProgram(shapeShaderId);

        shapeProjMatrixU = gl.glGetUniformLocation(shapeShaderId, "u_projMatrix");
        shapeModelViewMatrixU = gl.glGetUniformLocation(shapeShaderId, "u_modelViewMatrix");
        int shapeLightDirectionU = gl.glGetUniformLocation(shapeShaderId, "u_lightDirection");

        gl.glUseProgram(shapeShaderId);
        gl.glUniform3f(shapeLightDirectionU, (float)lightDirection.getX(), (float)lightDirection.getY(), (float)lightDirection.getZ());

        // Initialize Ground


        // Initialize Sphere
        Sphere sphereShape = new Sphere();
        spherePosition = new Point3D(0.0, 1.0, 0.0);

        int[] bufferId = new int[1];
        gl.glGenBuffers(1, bufferId, 0);
        sphereIBO = bufferId[0];
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, sphereIBO);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, sphereShape.getIndices().length * 4, Buffers.newDirectIntBuffer(sphereShape.getIndices()), GL.GL_STATIC_DRAW);
        sphereIndexCount = sphereShape.getIndices().length;

        float[] spherePositions = new float[sphereShape.getVertices().length * 3];
        float[] sphereNormals = new float[sphereShape.getVertices().length * 3];
        float[] sphereTexCoords = new float[sphereShape.getVertices().length * 2];

        for(int i=0; i<sphereShape.getVertices().length; ++i) {
            Vertex3D vertex = sphereShape.getVertices()[i];
            spherePositions[i * 3 + 0] = (float)vertex.getLocation().getX();
            spherePositions[i * 3 + 1] = (float)vertex.getLocation().getY();
            spherePositions[i * 3 + 2] = (float)vertex.getLocation().getZ();
            sphereNormals[i * 3 + 0] = (float)vertex.getNormalX();
            sphereNormals[i * 3 + 1] = (float)vertex.getNormalY();
            sphereNormals[i * 3 + 2] = (float)vertex.getNormalZ();
            sphereTexCoords[i * 2 + 0] = vertex.getFloatTextureCoords()[0];
            sphereTexCoords[i * 2 + 1] = vertex.getFloatTextureCoords()[1];
        }

        gl.glGenVertexArrays(1, bufferId, 0);
        sphereVAO = bufferId[0];
        gl.glBindVertexArray(sphereVAO);

        gl.glGenBuffers(1, bufferId, 0);
        int spherePositionsVBO = bufferId[0];
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, spherePositionsVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, spherePositions.length * 4, Buffers.newDirectFloatBuffer(spherePositions), GL.GL_STATIC_DRAW);

        gl.glVertexAttribPointer(0, 3,  GL.GL_FLOAT, false, 3 * 4, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glGenBuffers(1, bufferId, 0);
        int sphereNormalsVBO = bufferId[0];
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, sphereNormalsVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, sphereNormals.length * 4, Buffers.newDirectFloatBuffer(sphereNormals), GL.GL_STATIC_DRAW);

        gl.glVertexAttribPointer(1, 3,  GL.GL_FLOAT, false, 3 * 4, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glGenBuffers(1, bufferId, 0);
        int sphereTexCoordsVBO = bufferId[0];
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, sphereTexCoordsVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, sphereTexCoords.length * 4, Buffers.newDirectFloatBuffer(sphereTexCoords), GL.GL_STATIC_DRAW);

        gl.glVertexAttribPointer(2, 2,  GL.GL_FLOAT, false, 2 * 4, 0);
        gl.glEnableVertexAttribArray(2);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        camera.input(keyDown);

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(shapeShaderId);
        gl.glUniformMatrix4fv(shapeProjMatrixU, 1, false, camera.getProjectionMatrix().getFloatValues(), 0);

        MatrixStack stack = new MatrixStack(1);
        stack.loadMatrix(camera.getViewMatrix());
        stack.translate(spherePosition.getX(), spherePosition.getY(), spherePosition.getZ());
        gl.glUniformMatrix4fv(shapeModelViewMatrixU, 1, false, stack.peek().getFloatValues(), 0);

        gl.glBindVertexArray(sphereVAO);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, sphereIBO);
        gl.glDrawElements(GL.GL_TRIANGLES, sphereIndexCount, GL.GL_UNSIGNED_INT, 0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
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
        new Starter();
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
}
