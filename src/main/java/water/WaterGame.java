package water;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import opengl.*;
import org.joml.*;
import utils.Camera;

import javax.swing.*;
import java.awt.event.*;
import java.nio.FloatBuffer;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class WaterGame extends JFrame implements GLEventListener, KeyListener {

    private int width;
    private int height;

    private GLCanvas canvas;
    private Animator animator;
    private boolean[] keyDown;

    private GLManager manager;

    private SkyboxManager skyboxManager;
    private TerrainManager terrainManager;
    private WaterManager waterManager;
    private UnderwaterManager underwaterManager;

    private GLTexture terrainHeightMap;
    private GLTexture terrainNormalMap;
    private GLTexture terrainTexture;
    private GLTexture foamTexture;
    private GLTextureCube skyboxTexture;

    private GLUniformBuffer lightUBO;
    private GLUniformBuffer cameraUBO;
    private GLUniformBuffer invCameraUBO;

    private GLFrameBuffer refractFBO;
    private GLFrameBuffer reflectFBO;
    private GLFrameBuffer sceneFBO;

    private Camera camera;

    public WaterGame(String title, int width, int height) {
        super(title);

        this.width = width;
        this.height = height;

        this.canvas = new GLCanvas();
        this.animator = new Animator(this.canvas);
        this.keyDown = new boolean[256];
        this.manager = new GLManager();

        this.camera = new Camera(width, height);
        this.camera.setPosition(new Vector3f(0.0f, 10.0f, 0.0f));

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

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);

        // Common
        GLSampler sampler = manager.createSampler(GLSampler.EdgeType.WRAP, true);

        // Uniforms
        cameraUBO = manager.createUniformBuffer();
        invCameraUBO = manager.createUniformBuffer();

        lightUBO = manager.createUniformBuffer();
        lightUBO.setData(Buffers.newDirectFloatBuffer(new float[]{ 0.0f, 1.0f, 1.0f, 0.0f }));

        // FrameBuffers
        refractFBO = manager.createFrameBuffer();
        refractFBO.addColorTexture(0, manager.createTexture(width, height, false, null));
        refractFBO.setDepthTexture(manager.createTexture(width, height, true, null));
        refractFBO.getColorTexture(0).setSampler(sampler);

        reflectFBO = manager.createFrameBuffer();
        reflectFBO.addColorTexture(0, manager.createTexture(width, height, false, null));
        reflectFBO.setDepthTexture(manager.createTexture(width, height, true, null));
        reflectFBO.getColorTexture(0).setSampler(sampler);

        sceneFBO = manager.createFrameBuffer();
        sceneFBO.addColorTexture(0, manager.createTexture(width, height, false, null));
        sceneFBO.setDepthTexture(manager.createTexture(width, height, true, null));
        sceneFBO.getColorTexture(0).setSampler(sampler);

        // Textures
        terrainHeightMap = manager.loadTexture("/terrain_height.png");
        terrainHeightMap.setSampler(sampler);

        terrainNormalMap = manager.loadTexture("/terrain_norm.png");
        terrainNormalMap.setSampler(sampler);

        terrainTexture = manager.loadTexture("/sand_color.png");
        terrainTexture.setSampler(sampler);

        foamTexture = manager.loadTexture("/foam.png");
        foamTexture.setSampler(sampler);

        skyboxTexture = manager.loadTextureCube("/skybox/");
        skyboxTexture.setSampler(sampler);

        // Skybox
        skyboxManager = new SkyboxManager(manager, cameraUBO, skyboxTexture);
        skyboxManager.init("/shader/skybox.vs.glsl", "/shader/skybox.fs.glsl");

        // Terrain
        terrainManager = new TerrainManager(manager, 128, cameraUBO, lightUBO, terrainHeightMap, terrainNormalMap, terrainTexture);
        terrainManager.init("/shader/terrain.vs.glsl", "/shader/terrain.fs.glsl");

        // Water
        waterManager = new WaterManager(manager, 512, camera, cameraUBO, lightUBO, refractFBO, reflectFBO, terrainHeightMap);
        waterManager.init("/shader/water.vs.glsl", "/shader/oceanwater.glsl");

        // Underwater
        underwaterManager = new UnderwaterManager(manager, camera, invCameraUBO, sceneFBO);
        underwaterManager.init("/shader/quad.vs.glsl", "/shader/underwater.fs.glsl");
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GLUtils.logDebug("Dispose");

        manager.dispose();
    }

    private float time = 0.0f;

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        camera.input(keyDown);

        GL4 gl = (GL4) GLContext.getCurrentGL();

        float waterLevel = 8.0f;

        // --- Reflect Scene

        // Flip Camera
        Vector3f camPos = new Vector3f(camera.getPosition());
        Matrix3fc camRot = new Matrix3f(camera.getRotation());
        camera.setPosition(new Vector3f(camPos.x, camPos.y - 2.0f * (camPos.y - waterLevel), camPos.z));
        Vector3f forward = camera.getForward().mul(1.0f, -1.0f, 1.0f, new Vector3f());
        Vector3f up = forward.cross(camera.getRight(), new Vector3f()).normalize();
        camera.setRotation(new Matrix3f(camera.getRight(), up, forward));
        camera.update();

        // Push Camera Data to UBO
        FloatBuffer cameraBuffer = Buffers.newDirectFloatBuffer(16 * 2);
        camera.getProjMatrix().get(0, cameraBuffer);
        camera.getViewMatrix().get(16, cameraBuffer);
        cameraUBO.setData(cameraBuffer);

        // Revert Camera
        camera.setRotation(camRot);
        camera.setPosition(camPos);
        camera.update();

        // Draw to FBO
        reflectFBO.bind();
        gl.glViewport(0, 0, width, height);

        manager.clear();

        skyboxManager.render();
        terrainManager.render(time, new Vector4f(0.0f, 1.0f, 0.0f, -waterLevel + 0.0f));

        reflectFBO.unbind();

        // --- Refract Scene

        // Push Camera Data to UBO
        camera.getProjMatrix().get(0, cameraBuffer);
        camera.getViewMatrix().get(16, cameraBuffer);
        cameraUBO.setData(cameraBuffer);

        // Draw to FBO
        refractFBO.bind();
        gl.glViewport(0, 0, width, height);

        manager.clear();

        skyboxManager.render();
        terrainManager.render(time, new Vector4f(0.0f, -1.0f, 0.0f, waterLevel + 2.0f));

        refractFBO.unbind();

        // -- Water

        // Draw Water
        sceneFBO.bind();

        manager.clear();

        skyboxManager.render();
        terrainManager.render(time, new Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
        waterManager.render(time);

        sceneFBO.unbind();

        // Draw Underwater FX
        camera.getProjMatrix().invert(new Matrix4f()).get(0, cameraBuffer);
        camera.getViewMatrix().invert(new Matrix4f()).get(16, cameraBuffer);
        invCameraUBO.setData(cameraBuffer);

        manager.clear();

        underwaterManager.render();

        time += 0.01f;
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

    public static void main(String[] args) {
        new WaterGame("OpenGL Water", 1280, 800);
    }
}
