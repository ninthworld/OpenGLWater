package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import utils.DataFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class GLManager {

    private List<GLObject> glObjects;

    public GLManager() {
        this.glObjects = new ArrayList<>();
    }

    public void dispose() {
        for (GLObject obj : glObjects) {
            obj.dispose();
        }
    }

    public GLFrameBuffer createFrameBuffer() {
        GLFrameBuffer frameBuffer = new GLFrameBuffer();
        glObjects.add(frameBuffer);
        return frameBuffer;
    }

    public GLIndexBuffer createIndexBuffer() {
        GLIndexBuffer indexBuffer = new GLIndexBuffer();
        glObjects.add(indexBuffer);
        return indexBuffer;
    }

    public GLSampler createSampler(GLSampler.EdgeType edge, boolean linear) {
        GLSampler sampler = new GLSampler();
        sampler.init(edge, linear);
        glObjects.add(sampler);
        return sampler;
    }

    public GLShader createShader(String vertexFile, String geometryFile, String fragmentFile) {
        GLShader shader = new GLShader();
        shader.init(readFile(vertexFile), readFile(geometryFile), readFile(fragmentFile));
        glObjects.add(shader);
        return shader;
    }

    public GLShader createShader(String vertexFile, String fragmentFile) {
        GLShader shader = new GLShader();
        shader.init(readFile(vertexFile), null, readFile(fragmentFile));
        glObjects.add(shader);
        return shader;
    }

    public GLTexture createTexture(int width, int height, boolean isDepth, Buffer data) {
        GLTexture texture = new GLTexture();
        texture.init(width, height, isDepth, data);
        glObjects.add(texture);
        return texture;
    }

    public GLTextureCube createTextureCube(int width, int height, Buffer[] data) {
        GLTextureCube texture = new GLTextureCube();
        texture.init(width, height, data);
        glObjects.add(texture);
        return texture;
    }

    public GLUniformBuffer createUniformBuffer() {
        GLUniformBuffer uniformBuffer = new GLUniformBuffer();
        glObjects.add(uniformBuffer);
        return uniformBuffer;
    }

    public GLVertexArray createVertexArray() {
        GLVertexArray vertexArray = new GLVertexArray();
        glObjects.add(vertexArray);
        return vertexArray;
    }

    public GLVertexBuffer createVertexBuffer(DataFormat format) {
        GLVertexBuffer vertexBuffer = new GLVertexBuffer();
        vertexBuffer.setFormat(format);
        glObjects.add(vertexBuffer);
        return vertexBuffer;
    }

    public void clear() {
        GL gl = GLUtils.getGL();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        GLUtils.checkError("glClear");
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

    public GLTexture loadTexture(String file) {
        GLTexture texture = null;
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream(file));
            byte[] data = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
            ByteBuffer buffer = Buffers.newDirectByteBuffer(data.length);
            for(int i=0; i<data.length; i+=4) buffer.put(data[i+3]).put(data[i+2]).put(data[i+1]).put(data[i]);
            buffer.rewind();
            texture = createTexture(img.getWidth(), img.getHeight(), false, buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    public GLTextureCube loadTextureCube(String folder) {
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
            texture = createTextureCube(imgs[0].getWidth(), imgs[0].getHeight(), buffers);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }
}
