package opengl;

import com.jogamp.opengl.*;

public class GLUtils {

    public static GL getGL() {
        GL gl = GLContext.getCurrentGL();
        return gl;
    }

    public static GL2 getGL2() {
        GL2 gl = (GL2) GLContext.getCurrentGL();
        return gl;
    }

    public static GL3 getGL3() {
        GL3 gl = (GL3) GLContext.getCurrentGL();
        return gl;
    }

    public static GL4 getGL4() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        return gl;
    }

    public static void checkError(String msg) {
        GL gl = getGL();
        switch (gl.glGetError()) {
            case GL.GL_INVALID_ENUM:
                logError("Invalid enum");
                break;
            case GL.GL_INVALID_VALUE:
                logError("Invalid value");
                break;
            case GL.GL_INVALID_OPERATION:
                logError("Invalid operation");
                break;
            case GL.GL_INVALID_FRAMEBUFFER_OPERATION:
                logError("Invalid framebuffer operation");
                break;
            case GL.GL_OUT_OF_MEMORY:
                logError("Out of memory");
                break;
        }
    }

    public static void logError(String msg) {
        System.err.println("GL Error: " + msg);
    }

    public static void logDebug(String msg) {
        System.out.println("GL Debug: " + msg);
    }
}
