package opengl;

import utils.DataFormat;

import java.nio.Buffer;
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

    public GLShader createShader(String vertexSrc, String geometrySrc, String fragmentSrc) {
        GLShader shader = new GLShader();
        shader.init(vertexSrc, geometrySrc, fragmentSrc);
        glObjects.add(shader);
        return shader;
    }

    public GLShader createShader(String vertexSrc, String fragmentSrc) {
        GLShader shader = new GLShader();
        shader.init(vertexSrc, null, fragmentSrc);
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
}
