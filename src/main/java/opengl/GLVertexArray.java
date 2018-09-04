package opengl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import utils.DataFormat;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GLVertexArray {

    private int vertexArrayId;
    private List<GLVertexBuffer> vertexBuffers;

    public GLVertexArray() {
        GL2 gl = GLUtils.getGL2();

        IntBuffer arrays = Buffers.newDirectIntBuffer(1);
        gl.glGenVertexArrays(1, arrays);
        GLUtils.checkError("glGenVertexArrays");
        this.vertexArrayId = arrays.get();
        this.vertexBuffers = new ArrayList<>();
    }

    public void dispose() {
        GL2 gl = GLUtils.getGL2();

        if(vertexArrayId != 0) {
            gl.glDeleteVertexArrays(1, new int[]{ vertexArrayId }, 0);
            GLUtils.checkError("glDeleteVertexArrays");
        }
    }

    public void bind() {
        GL2 gl = GLUtils.getGL2();

        gl.glBindVertexArray(vertexArrayId);
        GLUtils.checkError("glBindVertexArray");

        for(int i = 0; i < vertexBuffers.size(); ++i) {
            gl.glEnableVertexAttribArray(i);
            GLUtils.checkError("glEnableVertexAttribArray");
        }
    }

    public void unbind() {
        GL2 gl = GLUtils.getGL2();

        for(int i = 0; i < vertexBuffers.size(); ++i) {
            gl.glDisableVertexAttribArray(i);
            GLUtils.checkError("glDisableVertexAttribArray");
        }

        gl.glBindVertexArray(0);
        GLUtils.checkError("glBindVertexArray");
    }

    public void addVertexBuffer(GLVertexBuffer buffer) {
        GL2 gl = GLUtils.getGL2();

        bind();
        buffer.bind();

        int offset = 0;
        Iterator<DataFormat.DataPair> it = buffer.getFormat().getFormatIterator();
        while(it.hasNext()) {
            DataFormat.DataPair pair = it.next();

            int dataType = 0;
            switch (pair.type) {
                case BYTE: dataType = GL.GL_BYTE; break;
                case SHORT: dataType = GL.GL_SHORT; break;
                case INT: dataType = GL2.GL_INT; break;
                case FLOAT: dataType = GL.GL_FLOAT; break;
                case DOUBLE: dataType = GL2.GL_DOUBLE; break;
            }

            int index = vertexBuffers.size();
            int byteSize = DataFormat.DataType.getByteSize(pair.type);
            gl.glVertexAttribPointer(index, byteSize, dataType, false, buffer.getFormat().getTotalByteLength(), offset);
            GLUtils.checkError("glVertexAttribPointer");

            offset += byteSize * pair.count;
        }

        buffer.unbind();
        unbind();

        vertexBuffers.add(buffer);
    }
}
