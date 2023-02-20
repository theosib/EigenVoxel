package org.theosib.GraphicsEngine;

import org.theosib.Adaptors.Disposable;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;

public class RenderingUtils {
    public static class Handles implements Disposable {
        public int VAO, VBO;

        @Override
        public void destroy() {
            if (VBO != 0) GL33.glDeleteBuffers(VBO);
            if (VAO != 0) GL33.glDeleteVertexArrays(VAO);
            VBO = 0;
            VAO = 0;
        }

        public void createVAO() {
            if (VAO == 0) VAO = GL33.glGenVertexArrays();
        }

        public void createVBO() {
            if (VBO == 0) VBO = GL33.glGenBuffers();
        }
    }

    static public Handles renderLines(Shader shader, FloatBuffer points, int numVertices, Handles handles) {
        if (handles == null) handles = new Handles();
        handles.createVAO();
        handles.createVBO();

        GL33.glBindVertexArray(handles.VAO);
        shader.bind();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, handles.VBO);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, points, GL33.GL_STATIC_DRAW);
        GL33.glVertexAttribPointer(0, 3, GL33.GL_FLOAT, false, 0, 0);
        GL33.glEnableVertexAttribArray(0);
        GL33.glDrawArrays(GL33.GL_LINES, 0, numVertices);
        GL33.glBindVertexArray(0);

        return handles;
    }

    static public Handles renderTriangles(Shader shader, FloatBuffer points, int numVertices, Handles handles) {
        if (handles == null) handles = new Handles();
        handles.createVAO();
        handles.createVBO();

        GL33.glBindVertexArray(handles.VAO);
        shader.bind();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, handles.VBO);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, points, GL33.GL_STATIC_DRAW);
        GL33.glVertexAttribPointer(0, 3, GL33.GL_FLOAT, false, 0, 0);
        GL33.glEnableVertexAttribArray(0);
        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, numVertices);
        GL33.glBindVertexArray(0);

        return handles;
    }

    static public void disableBlend() {
        GL33.glDisable(GL33.GL_BLEND);
    }

    static public void enableBlend() {
        GL33.glEnable(GL33.GL_BLEND);
        // assumes textures always have pre-multiplied alpha
        GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE_MINUS_SRC_ALPHA);
    }

}
