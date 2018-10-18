package tutorial0;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
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

    private Texture skyboxTexture;
    private int skyboxVAO;
    private int skyboxIBO;
    private int skyboxShaderId;
    private int u_skyboxProjMatrix;
    private int u_skyboxModelViewMatrix;
    private int u_skyboxTexture;

    private Texture groundTexture;
    private int groundVAO;
    private int shapeShaderId;
    private int u_shapeProjMatrix;
    private int u_shapeModelViewMatrix;
    private int u_shapeTexture;

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
        camera.move(8.0f, camera.getUp());

        // Initialize Scene
        initializeGround(gl);
        initializeSkybox(gl);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        camera.input(keyDown);

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Render Skybox
        gl.glUseProgram(skyboxShaderId);
        gl.glUniformMatrix4fv(u_skyboxProjMatrix, 1, false, camera.getProjectionMatrix().getFloatValues(), 0);
        gl.glUniformMatrix4fv(u_skyboxModelViewMatrix, 1, false, camera.getViewMatrix().getFloatValues(), 0);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, skyboxTexture.getTextureObject());
        gl.glUniform1i(u_skyboxTexture, 0);

        gl.glBindVertexArray(skyboxVAO);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, skyboxIBO);
        gl.glDrawElements(GL.GL_TRIANGLES, 36, GL.GL_UNSIGNED_INT, 0);

        // Render Ground
        gl.glUseProgram(shapeShaderId);
        gl.glUniformMatrix4fv(u_shapeProjMatrix, 1, false, camera.getProjectionMatrix().getFloatValues(), 0);
        gl.glUniformMatrix4fv(u_shapeModelViewMatrix, 1, false, camera.getViewMatrix().getFloatValues(), 0);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, groundTexture.getTextureObject());
        gl.glUniform1i(u_shapeTexture, 0);

        gl.glBindVertexArray(groundVAO);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
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

    private void initializeGround(GL2 gl) {
        // Load Grid Texture
        try {
            groundTexture = TextureIO.newTexture(getClass().getResource("/grid.png"), true, "png");
            groundTexture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
            groundTexture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
            groundTexture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_MIRRORED_REPEAT);
            groundTexture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_MIRRORED_REPEAT);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        u_shapeProjMatrix = gl.glGetUniformLocation(shapeShaderId, "u_projMatrix");
        u_shapeModelViewMatrix = gl.glGetUniformLocation(shapeShaderId, "u_modelViewMatrix");
        u_shapeTexture = gl.glGetUniformLocation(shapeShaderId, "u_colorTexture");
        int shapeLightDirectionU = gl.glGetUniformLocation(shapeShaderId, "u_lightDirection");

        gl.glUseProgram(shapeShaderId);
        gl.glUniform3f(shapeLightDirectionU, (float)lightDirection.getX(), (float)lightDirection.getY(), (float)lightDirection.getZ());

        // Initialize Ground
        float groundSize = 128.0f;
        float[] groundPositions = new float[] {
                -groundSize, 0.0f, -groundSize,
                -groundSize, 0.0f, groundSize,
                groundSize, 0.0f, -groundSize,
                groundSize, 0.0f, -groundSize,
                -groundSize, 0.0f, groundSize,
                groundSize, 0.0f, groundSize
        };
        float[] groundTexCoords = new float[] {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
        };

        int[] bufferId = new int[1];
        gl.glGenVertexArrays(1, bufferId, 0);
        groundVAO = bufferId[0];
        gl.glBindVertexArray(groundVAO);

        gl.glGenBuffers(1, bufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, groundPositions.length * 4, Buffers.newDirectFloatBuffer(groundPositions), GL.GL_STATIC_DRAW);

        gl.glVertexAttribPointer(0, 3,  GL.GL_FLOAT, false, 3 * 4, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glGenBuffers(1, bufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, groundTexCoords.length * 4, Buffers.newDirectFloatBuffer(groundTexCoords), GL.GL_STATIC_DRAW);

        gl.glVertexAttribPointer(1, 2,  GL.GL_FLOAT, false, 2 * 4, 0);
        gl.glEnableVertexAttribArray(1);
    }

    private void initializeSkybox(GL2 gl) {
        // Load Skybox Texture
        try {
            skyboxTexture = TextureIO.newTexture(GL.GL_TEXTURE_CUBE_MAP);
            GLProfile profile = GLContext.getCurrentGL().getGLProfile();
            skyboxTexture.updateImage(gl, TextureIO.newTextureData(profile, getClass().getResource("/skybox/front.png"), true, "png"), GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z);
            skyboxTexture.updateImage(gl, TextureIO.newTextureData(profile, getClass().getResource("/skybox/back.png"), true, "png"), GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
            skyboxTexture.updateImage(gl, TextureIO.newTextureData(profile, getClass().getResource("/skybox/right.png"), true, "png"), GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X);
            skyboxTexture.updateImage(gl, TextureIO.newTextureData(profile, getClass().getResource("/skybox/left.png"), true, "png"), GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X);
            skyboxTexture.updateImage(gl, TextureIO.newTextureData(profile, getClass().getResource("/skybox/top.png"), true, "png"), GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y);
            skyboxTexture.updateImage(gl, TextureIO.newTextureData(profile, getClass().getResource("/skybox/bottom.png"), true, "png"), GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y);
            skyboxTexture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
            skyboxTexture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
            skyboxTexture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_MIRRORED_REPEAT);
            skyboxTexture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_MIRRORED_REPEAT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load Skybox Shader
        skyboxShaderId = gl.glCreateProgram();

        int shaderId;
        shaderId = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        gl.glShaderSource(shaderId, 1, new String[]{ readFile("/tutorial0/skybox.vs.glsl") }, null);
        gl.glCompileShader(shaderId);
        gl.glAttachShader(skyboxShaderId, shaderId);

        shaderId = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        gl.glShaderSource(shaderId, 1, new String[]{ readFile("/tutorial0/skybox.fs.glsl") }, null);
        gl.glCompileShader(shaderId);
        gl.glAttachShader(skyboxShaderId, shaderId);

        gl.glLinkProgram(skyboxShaderId);

        u_skyboxProjMatrix = gl.glGetUniformLocation(skyboxShaderId, "u_projMatrix");
        u_skyboxModelViewMatrix = gl.glGetUniformLocation(skyboxShaderId, "u_modelViewMatrix");
        u_skyboxTexture = gl.glGetUniformLocation(skyboxShaderId, "u_skyboxTexture");

        // Initialize Skybox
        float skyboxSize = 256.0f;
        float[] skyboxPositions = new float[] {
                skyboxSize, skyboxSize, skyboxSize,
                skyboxSize, skyboxSize, -skyboxSize,
                skyboxSize, -skyboxSize, skyboxSize,
                skyboxSize, -skyboxSize, -skyboxSize,
                -skyboxSize, skyboxSize, skyboxSize,
                -skyboxSize, skyboxSize, -skyboxSize,
                -skyboxSize, -skyboxSize, skyboxSize,
                -skyboxSize, -skyboxSize, -skyboxSize
        };
        int[] skyboxIndices = new int[] {
                0, 4, 1, 4, 5, 1,
                2, 3, 6, 3, 7, 6,
                0, 6, 4, 6, 0, 2,
                1, 5, 7, 7, 3, 1,
                0, 1, 3, 3, 2, 0,
                4, 7, 5, 7, 4, 6
        };

        int[] bufferId = new int[1];
        gl.glGenBuffers(1, bufferId, 0);
        skyboxIBO = bufferId[0];
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, skyboxIBO);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, skyboxIndices.length * 4, Buffers.newDirectIntBuffer(skyboxIndices), GL.GL_STATIC_DRAW);

        gl.glGenVertexArrays(1, bufferId, 0);
        skyboxVAO = bufferId[0];
        gl.glBindVertexArray(skyboxVAO);

        gl.glGenBuffers(1, bufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, skyboxPositions.length * 4, Buffers.newDirectFloatBuffer(skyboxPositions), GL.GL_STATIC_DRAW);

        gl.glVertexAttribPointer(0, 3,  GL.GL_FLOAT, false, 3 * 4, 0);
        gl.glEnableVertexAttribArray(0);
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
}
