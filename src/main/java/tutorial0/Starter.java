package tutorial0;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class Starter extends JFrame implements GLEventListener, KeyListener {

    private GLCanvas canvas;
    private FPSAnimator animator;
    private boolean[] keyDown;

    private Camera camera;
    private Vector3f sunLightDirection;

    private Texture skyboxTexture;
    private Texture groundTexture;

    private int skyboxShaderProgram;
    private int skyboxVertexArray;
    private int skyboxIndexBuffer;

    private int geometryShaderProgram;
    private int groundVertexArray;

    public Starter(int width, int height) {
        super("Tutorial 0 - A Simple Scene");

        this.sunLightDirection = new Vector3f(0.2f, 0.5f, -1.0f).normalize();
        this.camera = new Camera();
        this.camera.initialize(width, height);
        this.camera.moveUp(12.0f);

        this.keyDown = new boolean[256];
        this.canvas = new GLCanvas();
        this.canvas.addGLEventListener(this);
        this.canvas.setFocusable(true);
        this.canvas.addKeyListener(this);
        this.add(this.canvas);

        this.setSize(width, height);
        this.setVisible(true);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.animator = new FPSAnimator(this.canvas, 60);
        this.animator.start();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = (GL2) GLContext.getCurrentGL();

        // Global Settings
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Load Textures
        skyboxTexture = Utils.loadTextureCube("target/classes/res/textures/skybox/", true, GL.GL_LINEAR_MIPMAP_LINEAR, GL.GL_CLAMP_TO_EDGE, 0.0f);
        groundTexture = Utils.loadTexture("target/classes/res/textures/grid.png", true, GL.GL_LINEAR_MIPMAP_LINEAR, GL.GL_REPEAT, 2.0f);

        // Load Shaders
        skyboxShaderProgram = Utils.createShader("target/classes/res/shaders/skybox.vs.glsl", "target/classes/res/shaders/skybox.fs.glsl");
        geometryShaderProgram = Utils.createShader("target/classes/tutorial0/geometry.vs.glsl", "target/classes/tutorial0/geometry.fs.glsl");

        // Initialize Skybox Geometry
        int[] bufferId = new int[1];
        gl.glGenBuffers(1, bufferId, 0);
        skyboxIndexBuffer = bufferId[0];
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, skyboxIndexBuffer);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, Utils.SKYBOX_INDICES.length * 4, Buffers.newDirectIntBuffer(Utils.SKYBOX_INDICES), GL.GL_STATIC_DRAW);

        gl.glGenVertexArrays(1, bufferId, 0);
        skyboxVertexArray = bufferId[0];
        gl.glBindVertexArray(skyboxVertexArray);

        gl.glGenBuffers(1, bufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, Utils.SKYBOX_POSITIONS.length * 4, Buffers.newDirectFloatBuffer(Utils.SKYBOX_POSITIONS), GL.GL_STATIC_DRAW);
        gl.glVertexAttribPointer(0, 3,  GL.GL_FLOAT, false, 3 * 4, 0);
        gl.glEnableVertexAttribArray(0);

        // Initialize Ground Geometry
        gl.glGenVertexArrays(1, bufferId, 0);
        groundVertexArray = bufferId[0];
        gl.glBindVertexArray(groundVertexArray);

        gl.glGenBuffers(1, bufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, Utils.PLANE_POSITIONS.length * 4, Buffers.newDirectFloatBuffer(Utils.PLANE_POSITIONS), GL.GL_STATIC_DRAW);
        gl.glVertexAttribPointer(0, 3,  GL.GL_FLOAT, false, 3 * 4, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glGenBuffers(1, bufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, Utils.PLANE_TEXCOORDS.length * 4, Buffers.newDirectFloatBuffer(Utils.PLANE_TEXCOORDS), GL.GL_STATIC_DRAW);
        gl.glVertexAttribPointer(1, 2,  GL.GL_FLOAT, false, 2 * 4, 0);
        gl.glEnableVertexAttribArray(1);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        // Input
        camera.input(keyDown);

        // Camera Matrices
        Matrix4fc projMatrix = camera.getProjMatrix();
        Matrix4fc viewMatrix = new Matrix4f(camera.getViewMatrix());

        // Clear Screen
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        drawSkybox(projMatrix, viewMatrix);
        drawGeometry(projMatrix, viewMatrix);
    }

    private void drawSkybox(Matrix4fc projMatrix, Matrix4fc viewMatrix) {
        GL2 gl = (GL2)GLContext.getCurrentGL();

        Matrix4f modelMatrix = new Matrix4f().identity();
        modelMatrix.scale(1000.0f);

        int skyboxProjMatrixU = gl.glGetUniformLocation(skyboxShaderProgram, "u_projMatrix");
        int skyboxModelViewMatrixU = gl.glGetUniformLocation(skyboxShaderProgram, "u_modelViewMatrix");
        int skyboxTextureU = gl.glGetUniformLocation(skyboxShaderProgram, "u_skyboxTexture");

        gl.glUseProgram(skyboxShaderProgram);

        FloatBuffer matrixBuffer = Buffers.newDirectFloatBuffer(16);
        viewMatrix.mul(modelMatrix, new Matrix4f()).get(matrixBuffer);
        gl.glUniformMatrix4fv(skyboxModelViewMatrixU, 1, false, matrixBuffer);

        projMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(skyboxProjMatrixU, 1, false, matrixBuffer);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, skyboxTexture.getTextureObject());
        gl.glUniform1i(skyboxTextureU, 0);

        gl.glBindVertexArray(skyboxVertexArray);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, skyboxIndexBuffer);
        gl.glDrawElements(GL.GL_TRIANGLES, 36, GL.GL_UNSIGNED_INT, 0);
    }

    private void drawGeometry(Matrix4fc projMatrix, Matrix4fc viewMatrix) {
        GL2 gl = (GL2)GLContext.getCurrentGL();

        Matrix4f modelMatrix = new Matrix4f().identity();
        modelMatrix.scale(128.0f);

        int geometryProjMatrixU = gl.glGetUniformLocation(geometryShaderProgram, "u_projMatrix");
        int geometryModelViewMatrixU = gl.glGetUniformLocation(geometryShaderProgram, "u_modelViewMatrix");
        int geometryTextureU = gl.glGetUniformLocation(geometryShaderProgram, "u_colorTexture");
        int geometrySunLightDirectionU = gl.glGetUniformLocation(geometryShaderProgram, "u_sunLightDirection");

        gl.glUseProgram(geometryShaderProgram);

        FloatBuffer matrixBuffer = Buffers.newDirectFloatBuffer(16);
        viewMatrix.mul(modelMatrix, new Matrix4f()).get(matrixBuffer);
        gl.glUniformMatrix4fv(geometryModelViewMatrixU, 1, false, matrixBuffer);

        projMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(geometryProjMatrixU, 1, false, matrixBuffer);

        gl.glUniform3f(geometrySunLightDirectionU, sunLightDirection.x, sunLightDirection.y, sunLightDirection.z);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, groundTexture.getTextureObject());
        gl.glUniform1i(geometryTextureU, 0);

        gl.glBindVertexArray(groundVertexArray);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        camera.initialize(width, height);
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

    public static void main(String[] args) {
        new Starter(1280, 960);
    }
}
