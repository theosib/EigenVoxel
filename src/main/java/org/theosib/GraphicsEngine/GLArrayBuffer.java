package org.theosib.GraphicsEngine;

import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;

public class GLArrayBuffer {
    int attribute_number;
    int VBO = 0;
    int num_components;

    public GLArrayBuffer(int an, int nc) {
        attribute_number = an;
        num_components = nc;
    }

    public void load(int VAO, FloatBuffer inBuf) {
        GL33.glBindVertexArray(VAO);
        if (VBO == 0) VBO = GL33.glGenBuffers();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, VBO);
        System.out.println("Limit:" + inBuf.limit() + " Rem: " + inBuf.remaining() + " Pos:" + inBuf.position() + " attr:" + attribute_number +
                " numc:" + num_components);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, inBuf, GL33.GL_STATIC_DRAW);
        // XXX use glBufferSubData
        GL33.glVertexAttribPointer(attribute_number, num_components, GL33.GL_FLOAT, false, 0, 0);
        GL33.glEnableVertexAttribArray(attribute_number);
        GL33.glBindVertexArray(0);
    }

    public void destroy() {
        if (VBO != 0) {
            GL33.glDeleteBuffers(VBO);
            VBO = 0;
        }
    }
}
