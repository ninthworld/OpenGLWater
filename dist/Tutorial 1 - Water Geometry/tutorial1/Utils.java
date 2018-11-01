package tutorial1;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;

public class Utils {

    private static final int INFO_LOG_BYTE_LENGTH = 1024;

    public static final float[] PLANE_POSITIONS = new float[] {
            -1.0f, 0.0f, -1.0f,
            -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f,
            1.0f, 0.0f, -1.0f,
            -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f
    };
    public static final float[] PLANE_TEXCOORDS = new float[] {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    public static final float[] SKYBOX_POSITIONS = new float[] {
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, -1.0f,
            -1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f
    };
    public static final int[] SKYBOX_INDICES = new int[] {
            0, 4, 1, 4, 5, 1,
            2, 3, 6, 3, 7, 6,
            0, 6, 4, 6, 0, 2,
            1, 5, 7, 7, 3, 1,
            0, 1, 3, 3, 2, 0,
            4, 7, 5, 7, 4, 6

    };


    public static Texture loadTexture(String textureFile, boolean mipmap, int filter, int edge, float anisotropy) {
        GL gl = GLContext.getCurrentGL();
        Texture texture = null;
        try {
            texture = TextureIO.newTexture(new File(textureFile), mipmap);
            texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, filter);
            texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, filter);
            texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, edge);
            texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, edge);
            if(mipmap && anisotropy > 0.0f) {
                texture.setTexParameterf(gl, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    public static Texture loadTextureCube(String textureDir, boolean mipmap, int filter, int edge, float anisotropy) {
        GL gl = GLContext.getCurrentGL();
        Texture texture = null;
        try {
            texture = TextureIO.newTexture(GL.GL_TEXTURE_CUBE_MAP);
            GLProfile profile = GLContext.getCurrentGL().getGLProfile();
            texture.updateImage(gl, TextureIO.newTextureData(profile, Paths.get(textureDir, "front.png").toFile(), true, "png"), GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z);
            texture.updateImage(gl, TextureIO.newTextureData(profile, Paths.get(textureDir, "back.png").toFile(), true, "png"), GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
            texture.updateImage(gl, TextureIO.newTextureData(profile, Paths.get(textureDir, "right.png").toFile(), true, "png"), GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X);
            texture.updateImage(gl, TextureIO.newTextureData(profile, Paths.get(textureDir, "left.png").toFile(), true, "png"), GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X);
            texture.updateImage(gl, TextureIO.newTextureData(profile, Paths.get(textureDir, "top.png").toFile(), true, "png"), GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y);
            texture.updateImage(gl, TextureIO.newTextureData(profile, Paths.get(textureDir, "bottom.png").toFile(), true, "png"), GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y);
            texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, filter);
            texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, filter);
            texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, edge);
            texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, edge);
            if(mipmap && anisotropy > 0.0f) {
                texture.setTexParameterf(gl, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    public static int createShader(String vertShaderFile, String fragShaderFile) {
        GL2 gl = (GL2) GLContext.getCurrentGL();

        int programId = gl.glCreateProgram();

        int vertShaderId = compileShader(readFile(vertShaderFile), GL2.GL_VERTEX_SHADER);
        gl.glAttachShader(programId, vertShaderId);

        int fragShaderId = compileShader(readFile(fragShaderFile), GL2.GL_FRAGMENT_SHADER);
        gl.glAttachShader(programId, fragShaderId);

        gl.glLinkProgram(programId);

        IntBuffer intBuffer = Buffers.newDirectIntBuffer(1);
        gl.glGetProgramiv(programId, GL2.GL_LINK_STATUS, intBuffer);
        if(intBuffer.get(0) == GL.GL_FALSE) {
            ByteBuffer infoLog = Buffers.newDirectByteBuffer(INFO_LOG_BYTE_LENGTH);
            gl.glGetProgramInfoLog(programId, INFO_LOG_BYTE_LENGTH, intBuffer, infoLog);
            byte[] infoLogData = new byte[INFO_LOG_BYTE_LENGTH];
            infoLog.get(infoLogData);
            System.err.println("Shader link error: " + new String(infoLogData));
        }

        gl.glDeleteShader(vertShaderId);
        gl.glDeleteShader(fragShaderId);

        return programId;
    }

    private static int compileShader(String shaderSrc, int shaderType) {
        GL2 gl = (GL2) GLContext.getCurrentGL();

        int shaderId = gl.glCreateShader(shaderType);
        gl.glShaderSource(shaderId, 1, new String[]{ shaderSrc }, null);
        gl.glCompileShader(shaderId);

        IntBuffer intBuffer = Buffers.newDirectIntBuffer(1);
        gl.glGetShaderiv(shaderId, GL2.GL_COMPILE_STATUS, intBuffer);
        if(intBuffer.get(0) == GL.GL_FALSE) {
            ByteBuffer infoLog = Buffers.newDirectByteBuffer(INFO_LOG_BYTE_LENGTH);
            gl.glGetShaderInfoLog(shaderId, INFO_LOG_BYTE_LENGTH, intBuffer, infoLog);
            byte[] infoLogData = new byte[INFO_LOG_BYTE_LENGTH];
            infoLog.get(infoLogData);
            System.err.println("Shader compile error: " + new String(infoLogData));
        }

        return shaderId;
    }

    private static String readFile(String file) {
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while((line=in.readLine()) != null) {
                builder.append(line).append("\n");
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }
}
