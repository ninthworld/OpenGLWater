package tutorial4;

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
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class Starter extends JFrame implements GLEventListener, KeyListener {

    private static final float WATER_LEVEL = 8.0f;

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

    private int waterShaderProgram;
    private int waterVertexArray;

    private int refractTextureId;
    private int reflectTextureId;
    private int refractFrameBuffer;
    private int reflectFrameBuffer;

    private int underwaterShaderProgram;
    private int quadVertexArray;

    private int sceneColorTextureId;
    private int sceneDepthTextureId;
    private int sceneFrameBuffer;

    private float time;
    private int[] noiseTextureIds;

    public Starter(int width, int height) {
        super("Tutorial 4 - Underwater Caustics");

        this.sunLightDirection = new Vector3f(0.2f, 0.5f, -1.0f).normalize();
        this.camera = new Camera();
        this.camera.initialize(width, height);
        this.camera.moveUp(12.0f);

        this.time = 0.0f;
        this.noiseTextureIds = new int[4];

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
        GL3 gl = (GL3) GLContext.getCurrentGL();

        // Global Settings
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Load Textures
        skyboxTexture = Utils.loadTextureCube("target/classes/res/textures/skybox/", true, GL.GL_LINEAR_MIPMAP_LINEAR, GL.GL_CLAMP_TO_EDGE, 0.0f);
        groundTexture = Utils.loadTexture("target/classes/res/textures/grid.png", true, GL.GL_LINEAR_MIPMAP_LINEAR, GL.GL_REPEAT, 2.0f);

        // Load Shaders
        skyboxShaderProgram = Utils.createShader("target/classes/res/shaders/skybox.vs.glsl", "target/classes/res/shaders/skybox.fs.glsl");
        geometryShaderProgram = Utils.createShader("target/classes/tutorial4/geometry.vs.glsl", "target/classes/tutorial4/geometry.fs.glsl");
        waterShaderProgram = Utils.createShader("target/classes/tutorial4/water.vs.glsl", "target/classes/tutorial4/water.fs.glsl");
        underwaterShaderProgram = Utils.createShader("target/classes/tutorial4/underwater.vs.glsl", "target/classes/tutorial4/underwater.fs.glsl");

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

        // Initialize Water Geometry
        gl.glGenVertexArrays(1, bufferId, 0);
        waterVertexArray = bufferId[0];
        gl.glBindVertexArray(waterVertexArray);

        gl.glGenBuffers(1, bufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, Utils.PLANE_POSITIONS.length * 4, Buffers.newDirectFloatBuffer(Utils.PLANE_POSITIONS), GL.GL_STATIC_DRAW);
        gl.glVertexAttribPointer(0, 3,  GL.GL_FLOAT, false, 3 * 4, 0);
        gl.glEnableVertexAttribArray(0);

        // Initialize Quad Geometry
        gl.glGenVertexArrays(1, bufferId, 0);
        quadVertexArray = bufferId[0];
        gl.glBindVertexArray(quadVertexArray);

        gl.glGenBuffers(1, bufferId, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, Utils.QUAD_POSITIONS.length * 4, Buffers.newDirectFloatBuffer(Utils.QUAD_POSITIONS), GL.GL_STATIC_DRAW);
        gl.glVertexAttribPointer(0, 2,  GL.GL_FLOAT, false, 2 * 4, 0);
        gl.glEnableVertexAttribArray(0);

        // Initialize Refract Framebuffer
        gl.glGenFramebuffers(1, bufferId, 0);
        refractFrameBuffer = bufferId[0];
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, refractFrameBuffer);
        gl.glGenTextures(1, bufferId, 0);
        refractTextureId = bufferId[0];
        gl.glBindTexture(GL.GL_TEXTURE_2D, refractTextureId);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, getWidth(), getHeight(), 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, refractTextureId, 0);
        gl.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);

        // Initialize Reflect Framebuffer
        gl.glGenFramebuffers(1, bufferId, 0);
        reflectFrameBuffer = bufferId[0];
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, reflectFrameBuffer);
        gl.glGenTextures(1, bufferId, 0);
        reflectTextureId = bufferId[0];
        gl.glBindTexture(GL.GL_TEXTURE_2D, reflectTextureId);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, getWidth(), getHeight(), 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, reflectTextureId, 0);
        gl.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);

        // Initialize Scene Framebuffer
        gl.glGenFramebuffers(1, bufferId, 0);
        sceneFrameBuffer = bufferId[0];
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, sceneFrameBuffer);
        gl.glGenTextures(1, bufferId, 0);
        sceneColorTextureId = bufferId[0];
        gl.glBindTexture(GL.GL_TEXTURE_2D, sceneColorTextureId);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, getWidth(), getHeight(), 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, sceneColorTextureId, 0);
        gl.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);
        gl.glGenTextures(1, bufferId, 0);
        sceneDepthTextureId = bufferId[0];
        gl.glBindTexture(GL.GL_TEXTURE_2D, sceneDepthTextureId);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_DEPTH_COMPONENT24, getWidth(), getHeight(), 0, GL2.GL_DEPTH_COMPONENT, GL.GL_FLOAT, null);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_TEXTURE_2D, sceneDepthTextureId, 0);

        // Initialize Noise Textures
        for(int o=0; o<noiseTextureIds.length; ++o) {
            Noise noise = new Noise();

            int pow = (int)Math.pow(2, o);
            ByteBuffer textureData = Buffers.newDirectByteBuffer(256 * 256 * 4);
            for(int i=0; i<256; ++i) {
                for(int j=0; j<256; ++j) {
                    float normalHeight = noise.noise(i * pow / 32.0f, j * pow / 32.0f, 8 * pow) * 0.5f + 0.5f;
                    byte val = (byte)(normalHeight * 256.0f);
                    textureData.put(val).put(val).put(val).put((byte)255);
                }
            }
            textureData.rewind();

            gl.glGenTextures(1, bufferId, 0);
            noiseTextureIds[o] = bufferId[0];
            gl.glBindTexture(GL.GL_TEXTURE_2D, noiseTextureIds[o]);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, 256, 256, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, textureData);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        }
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

        Matrix4f newRotation = new Matrix4f().identity().set(camera.getRotation());
        newRotation.m10(-newRotation.m10());
        newRotation.m12(-newRotation.m12());
        newRotation.m21(-newRotation.m21());

        Vector3f newPosition = new Vector3f(camera.getPosition());
        newPosition.y = 2.0f * WATER_LEVEL - newPosition.y;

        Matrix4f reflectViewMatrix = new Matrix4f().identity();
        reflectViewMatrix.translate(newPosition);
        reflectViewMatrix.mul(newRotation);
        reflectViewMatrix.invert();

        // Bind Reflect Framebuffer
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, reflectFrameBuffer);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(0, 0, getWidth(), getHeight());

        drawSkybox(projMatrix, reflectViewMatrix);
        drawGeometry(projMatrix, reflectViewMatrix);

        // Bind Refract Framebuffer
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, refractFrameBuffer);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(0, 0, getWidth(), getHeight());

        drawSkybox(projMatrix, viewMatrix);
        drawGeometry(projMatrix, viewMatrix);

        // Bind Scene Framebuffer
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, sceneFrameBuffer);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(0, 0, getWidth(), getHeight());

        drawSkybox(projMatrix, viewMatrix);
        drawGeometry(projMatrix, viewMatrix);

        // Render Water
        Matrix4f modelMatrix = new Matrix4f().identity();
        modelMatrix.translate(0.0f, WATER_LEVEL, 0.0f);
        modelMatrix.scale(128.0f);

        int waterProjMatrixU = gl.glGetUniformLocation(waterShaderProgram, "u_projMatrix");
        int waterViewMatrixU = gl.glGetUniformLocation(waterShaderProgram, "u_viewMatrix");
        int waterModelMatrixU = gl.glGetUniformLocation(waterShaderProgram, "u_modelMatrix");
        int waterCameraPositionU = gl.glGetUniformLocation(waterShaderProgram, "u_cameraPosition");
        int waterSunLightDirectionU = gl.glGetUniformLocation(waterShaderProgram, "u_sunLightDirection");
        int waterRefractTextureU = gl.glGetUniformLocation(waterShaderProgram, "u_refractTexture");
        int waterReflectTextureU = gl.glGetUniformLocation(waterShaderProgram, "u_reflectTexture");
        int waterTimeU = gl.glGetUniformLocation(waterShaderProgram, "u_time");
        int waterWaterLevelU = gl.glGetUniformLocation(waterShaderProgram, "u_waterLevel");

        gl.glUseProgram(waterShaderProgram);

        FloatBuffer matrixBuffer = Buffers.newDirectFloatBuffer(16);
        viewMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(waterViewMatrixU, 1, false, matrixBuffer);

        modelMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(waterModelMatrixU, 1, false, matrixBuffer);

        projMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(waterProjMatrixU, 1, false, matrixBuffer);

        gl.glUniform3f(waterCameraPositionU, camera.getPosition().x(), camera.getPosition().y(), camera.getPosition().z());
        gl.glUniform3f(waterSunLightDirectionU, sunLightDirection.x, sunLightDirection.y, sunLightDirection.z);
        gl.glUniform1f(waterTimeU, time);
        gl.glUniform1f(waterWaterLevelU, WATER_LEVEL);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, refractTextureId);
        gl.glUniform1i(waterRefractTextureU, 0);
        gl.glActiveTexture(GL.GL_TEXTURE1);
        gl.glBindTexture(GL.GL_TEXTURE_2D, reflectTextureId);
        gl.glUniform1i(waterReflectTextureU, 1);

        for(int i=0; i<noiseTextureIds.length; ++i) {
            int waterNoiseTextureU = gl.glGetUniformLocation(waterShaderProgram, "u_noiseTexture[" + i + "]");
            gl.glActiveTexture(GL.GL_TEXTURE2 + i);
            gl.glBindTexture(GL.GL_TEXTURE_2D, noiseTextureIds[i]);
            gl.glUniform1i(waterNoiseTextureU, 2 + i);
        }

        gl.glDisable(GL.GL_CULL_FACE);
        gl.glBindVertexArray(waterVertexArray);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl.glEnable(GL.GL_CULL_FACE);

        time += 1.0f;

        // Unbind Framebuffer
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

        // Clear Screen
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(underwaterShaderProgram);

        int underwaterWaterLevelU = gl.glGetUniformLocation(underwaterShaderProgram, "u_waterLevel");
        int underwaterCameraPositionU = gl.glGetUniformLocation(underwaterShaderProgram, "u_cameraPosition");
        int underwaterColorTextureU = gl.glGetUniformLocation(underwaterShaderProgram, "u_colorTexture");
        int underwaterDepthTextureU = gl.glGetUniformLocation(underwaterShaderProgram, "u_depthTexture");

        gl.glUniform1f(underwaterWaterLevelU, WATER_LEVEL);
        gl.glUniform3f(underwaterCameraPositionU, camera.getPosition().x(), camera.getPosition().y(), camera.getPosition().z());

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, sceneColorTextureId);
        gl.glUniform1i(underwaterColorTextureU, 0);
        gl.glActiveTexture(GL.GL_TEXTURE1);
        gl.glBindTexture(GL.GL_TEXTURE_2D, sceneDepthTextureId);
        gl.glUniform1i(underwaterDepthTextureU, 1);

        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glBindVertexArray(quadVertexArray);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl.glEnable(GL.GL_DEPTH_TEST);

    }

    private void drawSkybox(Matrix4fc projMatrix, Matrix4fc viewMatrix) {
        GL2 gl = (GL2)GLContext.getCurrentGL();

        int skyboxProjMatrixU = gl.glGetUniformLocation(skyboxShaderProgram, "u_projMatrix");
        int skyboxModelViewMatrixU = gl.glGetUniformLocation(skyboxShaderProgram, "u_modelViewMatrix");
        int skyboxTextureU = gl.glGetUniformLocation(skyboxShaderProgram, "u_skyboxTexture");

        gl.glUseProgram(skyboxShaderProgram);

        FloatBuffer matrixBuffer = Buffers.newDirectFloatBuffer(16);
        viewMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(skyboxModelViewMatrixU, 1, false, matrixBuffer);

        projMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(skyboxProjMatrixU, 1, false, matrixBuffer);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, skyboxTexture.getTextureObject());
        gl.glUniform1i(skyboxTextureU, 0);

        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glBindVertexArray(skyboxVertexArray);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, skyboxIndexBuffer);
        gl.glDrawElements(GL.GL_TRIANGLES, 36, GL.GL_UNSIGNED_INT, 0);
        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    private void drawGeometry(Matrix4fc projMatrix, Matrix4fc viewMatrix) {
        GL2 gl = (GL2)GLContext.getCurrentGL();

        Matrix4f modelMatrix = new Matrix4f().identity();
        modelMatrix.scale(128.0f);

        int geometryProjMatrixU = gl.glGetUniformLocation(geometryShaderProgram, "u_projMatrix");
        int geometryViewMatrixU = gl.glGetUniformLocation(geometryShaderProgram, "u_viewMatrix");
        int geometryModelMatrixU = gl.glGetUniformLocation(geometryShaderProgram, "u_modelMatrix");
        int geometryTextureU = gl.glGetUniformLocation(geometryShaderProgram, "u_colorTexture");
        int geometrySunLightDirectionU = gl.glGetUniformLocation(geometryShaderProgram, "u_sunLightDirection");
        int geometryWaterLevelU = gl.glGetUniformLocation(geometryShaderProgram, "u_waterLevel");
        int geometryTimeU = gl.glGetUniformLocation(geometryShaderProgram, "u_time");

        gl.glUseProgram(geometryShaderProgram);

        FloatBuffer matrixBuffer = Buffers.newDirectFloatBuffer(16);
        viewMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(geometryViewMatrixU, 1, false, matrixBuffer);

        modelMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(geometryModelMatrixU, 1, false, matrixBuffer);

        projMatrix.get(matrixBuffer);
        gl.glUniformMatrix4fv(geometryProjMatrixU, 1, false, matrixBuffer);

        gl.glUniform1f(geometryWaterLevelU, WATER_LEVEL);
        gl.glUniform1f(geometryTimeU, time);
        gl.glUniform3f(geometrySunLightDirectionU, sunLightDirection.x, sunLightDirection.y, sunLightDirection.z);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, groundTexture.getTextureObject());
        gl.glUniform1i(geometryTextureU, 0);

        for(int i=0; i<noiseTextureIds.length; ++i) {
            int geometryNoiseTextureU = gl.glGetUniformLocation(geometryShaderProgram, "u_noiseTexture[" + i + "]");
            gl.glActiveTexture(GL.GL_TEXTURE1 + i);
            gl.glBindTexture(GL.GL_TEXTURE_2D, noiseTextureIds[i]);
            gl.glUniform1i(geometryNoiseTextureU, 1 + i);
        }

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
