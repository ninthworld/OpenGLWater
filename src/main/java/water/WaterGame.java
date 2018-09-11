package water;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import opengl.*;
import org.joml.*;
import utils.Camera;
import utils.DataFormat;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static java.awt.event.KeyEvent.VK_ESCAPE;

public class WaterGame extends JFrame implements GLEventListener, KeyListener {

    private int width;
    private int height;

    private GLCanvas canvas;
    private Animator animator;
    private boolean[] keyDown;

    private GLManager manager;

    private GLVertexArray terrainVAO;
    private GLVertexArray skyboxVAO;
    private GLVertexArray waterVAO;

    private GLIndexBuffer terrainIBO;
    private GLIndexBuffer skyboxIBO;
    private GLIndexBuffer waterIBO;

    private GLTexture terrainHeightMap;
    private GLTexture terrainNormalMap;
    private GLTexture terrainTexture;
    private GLTexture foamTexture;
    private GLTextureCube skyboxTexture;

    private GLShader terrainShader;
    private GLShader skyboxShader;
    private GLShader waterShader;

    private GLUniformBuffer lightUBO;
    private GLUniformBuffer cameraUBO;

    private GLFrameBuffer refractFBO;
    private GLFrameBuffer reflectFBO;

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

        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);

        // Common
        GLSampler sampler = manager.createSampler(GLSampler.EdgeType.WRAP, true);

        // Uniforms
        cameraUBO = manager.createUniformBuffer();

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

        // Textures
        terrainHeightMap = loadTexture("/terrain_height.png");
        terrainHeightMap.setSampler(sampler);

        terrainNormalMap = loadTexture("/terrain_norm.png");
        terrainNormalMap.setSampler(sampler);

        terrainTexture = loadTexture("/sand_color.png");
        terrainTexture.setSampler(sampler);

        foamTexture = loadTexture("/foam.png");
        foamTexture.setSampler(sampler);

        skyboxTexture = loadTextureCube("/skybox/");
        skyboxTexture.setSampler(sampler);

        // Skybox
        skyboxShader = manager.createShader(readFile("/shader/skybox.vs.glsl"), readFile("/shader/skybox.fs.glsl"));
        skyboxShader.addUniformBuffer(0, cameraUBO);
        skyboxShader.addTexture(0, skyboxTexture);

        skyboxVAO = manager.createVertexArray();
        skyboxIBO = manager.createIndexBuffer();
        GLVertexBuffer skyboxVBO = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 3));
        {
            FloatBuffer vertices = Buffers.newDirectFloatBuffer(new float[]{
                    1.0f, 1.0f, 1.0f,
                    1.0f, 1.0f, -1.0f,
                    1.0f, -1.0f, 1.0f,
                    1.0f, -1.0f, -1.0f,
                    -1.0f, 1.0f, 1.0f,
                    -1.0f, 1.0f, -1.0f,
                    -1.0f, -1.0f, 1.0f,
                    -1.0f, -1.0f, -1.0f
            });
            skyboxVBO.setData(vertices);
            skyboxVAO.addVertexBuffer(skyboxVBO);

            IntBuffer indices = Buffers.newDirectIntBuffer(new int[]{
                    0, 4, 1, 4, 5, 1,
                    2, 3, 6, 3, 7, 6,
                    0, 6, 4, 6, 0, 2,
                    1, 5, 7, 7, 3, 1,
                    0, 1, 3, 3, 2, 0,
                    4, 7, 5, 7, 4, 6
            });
            skyboxIBO.setData(indices);
            skyboxIBO.setCount(36);
        }

        // Terrain
        terrainShader = manager.createShader(readFile("/shader/terrain.vs.glsl"), readFile("/shader/terrain.fs.glsl"));
        terrainShader.addUniformBuffer(0, cameraUBO);
        terrainShader.addUniformBuffer(1, lightUBO);
        terrainShader.addTexture(0, terrainHeightMap);
        terrainShader.addTexture(1, terrainNormalMap);
        terrainShader.addTexture(2, terrainTexture);

        terrainVAO = manager.createVertexArray();
        terrainIBO = manager.createIndexBuffer();
        GLVertexBuffer terrainVBO = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 3));
        {
            int segments = 128;
            FloatBuffer vertices = Buffers.newDirectFloatBuffer((segments + 1) * (segments + 1) * 3);
            for(int i=0; i <= segments; ++i) {
                float x = (float) i / (float) segments;
                for(int j=0; j <= segments; ++j) {
                    float z = (float)j / (float) segments;
                    vertices.put(x).put(0.0f).put(z);
                }
            }
            vertices.rewind();
            terrainVBO.setData(vertices);
            terrainVAO.addVertexBuffer(terrainVBO);

            IntBuffer indices = Buffers.newDirectIntBuffer(segments * segments * 6);
            for(int i=0; i<segments; ++i) {
                for(int j=0; j<segments; ++j) {
                    indices.put(i + j * (segments + 1)).put(i + 1 + j * (segments + 1)).put(i + 1 + (j + 1) * (segments + 1));
                    indices.put(i + 1 + (j + 1) * (segments + 1)).put(i + (j + 1) * (segments + 1)).put(i + j * (segments + 1));
                }
            }
            indices.rewind();
            terrainIBO.setData(indices);
            terrainIBO.setCount(segments * segments * 6);
        }

        // Water
        waterShader = manager.createShader(readFile("/shader/water.vs.glsl"), readFile("/shader/water.fs.glsl"));
        waterShader.addUniformBuffer(0, cameraUBO);
        waterShader.addUniformBuffer(1, lightUBO);
        waterShader.addTexture(0, refractFBO.getColorTexture(0));
        waterShader.addTexture(1, reflectFBO.getColorTexture(0));
        waterShader.addTexture(2, terrainHeightMap);
        waterShader.addTexture(3, foamTexture);

        waterVAO = manager.createVertexArray();
        waterIBO = manager.createIndexBuffer();
        GLVertexBuffer waterVBO = manager.createVertexBuffer(new DataFormat().add(DataFormat.DataType.FLOAT, 3));
        {
            int segments = 128;
            FloatBuffer vertices = Buffers.newDirectFloatBuffer((segments + 1) * (segments + 1) * 3);
            for(int i=0; i <= segments; ++i) {
                float x = (float) i / (float) segments;
                for(int j=0; j <= segments; ++j) {
                    float z = (float)j / (float) segments;
                    vertices.put(x).put(0.0f).put(z);
                }
            }
            vertices.rewind();
            waterVBO.setData(vertices);
            waterVAO.addVertexBuffer(waterVBO);

            IntBuffer indices = Buffers.newDirectIntBuffer(segments * segments * 6);
            for(int i=0; i<segments; ++i) {
                for(int j=0; j<segments; ++j) {
                    indices.put(i + j * (segments + 1)).put(i + 1 + j * (segments + 1)).put(i + 1 + (j + 1) * (segments + 1));
                    indices.put(i + 1 + (j + 1) * (segments + 1)).put(i + (j + 1) * (segments + 1)).put(i + j * (segments + 1));
                }
            }
            indices.rewind();
            waterIBO.setData(indices);
            waterIBO.setCount(segments * segments * 6);
        }
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

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        GLUtils.checkError("glClear");

        gl.glEnable(GL3.GL_CLIP_DISTANCE0);
        terrainShader.setUniform4f(1, new Vector4f(0.0f, 1.0f, 0.0f, -waterLevel + 0.0f));
        renderScene(gl);
        gl.glDisable(GL3.GL_CLIP_DISTANCE0);

        reflectFBO.unbind();

        // --- Refract Scene

        // Push Camera Data to UBO
        camera.getProjMatrix().get(0, cameraBuffer);
        camera.getViewMatrix().get(16, cameraBuffer);
        cameraUBO.setData(cameraBuffer);

        // Draw to FBO
        refractFBO.bind();
        gl.glViewport(0, 0, width, height);

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        GLUtils.checkError("glClear");

        gl.glEnable(GL3.GL_CLIP_DISTANCE0);
        terrainShader.setUniform4f(1, new Vector4f(0.0f, -1.0f, 0.0f, waterLevel + 2.0f));
        renderScene(gl);
        gl.glDisable(GL3.GL_CLIP_DISTANCE0);

        refractFBO.unbind();

        // -- Water

        // Draw Water
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        GLUtils.checkError("glClear");

        renderScene(gl);

        waterShader.setUniform4f(4, new Vector4f(camera.getPosition(), 0.0f));
        waterShader.setUniform1f(5, time += 0.01f);

        gl.glEnable(GL.GL_DEPTH_TEST);
        waterShader.bind();
        waterVAO.bind();
        waterIBO.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, waterIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        GLUtils.checkError("glDrawArrays");
        waterIBO.unbind();
        waterVAO.unbind();
        waterShader.unbind();
        gl.glDisable(GL.GL_DEPTH_TEST);
    }

    public void renderScene(GL4 gl) {
        // Skybox
        skyboxShader.bind();
        skyboxVAO.bind();
        skyboxIBO.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, skyboxIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        GLUtils.checkError("glDrawArrays");
        skyboxIBO.unbind();
        skyboxVAO.unbind();
        skyboxShader.unbind();

        // Terrain
        gl.glEnable(GL.GL_DEPTH_TEST);
        terrainShader.bind();
        terrainVAO.bind();
        terrainIBO.bind();
        gl.glDrawElements(GL.GL_TRIANGLES, terrainIBO.getCount(), GL.GL_UNSIGNED_INT, 0);
        GLUtils.checkError("glDrawArrays");
        terrainIBO.unbind();
        terrainVAO.unbind();
        terrainShader.unbind();
        gl.glDisable(GL.GL_DEPTH_TEST);
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

    private GLTexture loadTexture(String file) {
        GLTexture texture = null;
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream(file));
            byte[] data = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
            ByteBuffer buffer = Buffers.newDirectByteBuffer(data.length);
            for(int i=0; i<data.length; i+=4) buffer.put(data[i+3]).put(data[i+2]).put(data[i+1]).put(data[i]);
            buffer.rewind();
            texture = manager.createTexture(img.getWidth(), img.getHeight(), false, buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    private GLTextureCube loadTextureCube(String folder) {
        GLTextureCube texture = null;
        try {
            BufferedImage[] imgs = new BufferedImage[6];
            imgs[0] = ImageIO.read(getClass().getResourceAsStream(folder + "right.png"));
            imgs[1] = ImageIO.read(getClass().getResourceAsStream(folder + "left.png"));
            imgs[2] = ImageIO.read(getClass().getResourceAsStream(folder + "top.png"));
            imgs[3] = ImageIO.read(getClass().getResourceAsStream(folder + "bottom.png"));
            imgs[4] = ImageIO.read(getClass().getResourceAsStream(folder + "back.png"));
            imgs[5] = ImageIO.read(getClass().getResourceAsStream(folder + "front.png"));
            ByteBuffer[] buffers = new ByteBuffer[6];
            for(int i=0; i<buffers.length; ++i) {
                byte[] data = ((DataBufferByte)imgs[i].getRaster().getDataBuffer()).getData();
                buffers[i] = Buffers.newDirectByteBuffer(data.length);
                for(int j=0; j<data.length; j+=4) buffers[i].put(data[j+3]).put(data[j+2]).put(data[j+1]).put(data[j]);
                buffers[i].rewind();
            }
            texture = manager.createTextureCube(imgs[0].getWidth(), imgs[0].getHeight(), buffers);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    public static void main(String[] args) {
        new WaterGame("OpenGL Water", 1280, 800);
    }
}
