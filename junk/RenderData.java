package org.theosib.GraphicsEngine;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class RenderData {
    FloatBuffer vertices = null;
    FloatBuffer texcoords = null;
    FloatBuffer normals = null;

    int num_vertices = 0;

    public FloatBuffer reserveVertexFloats(int num_floats) {
        if (vertices == null) {
            vertices = MemoryUtil.memAllocFloat(num_floats);
            return vertices;
        }
        if (vertices.capacity() < num_floats) {
            MemoryUtil.memFree(vertices);
            vertices = MemoryUtil.memAllocFloat(num_floats);
            return vertices;
        }
        vertices.limit(num_floats);
        return vertices;
    }

    public FloatBuffer reserveTexcoordFloats(int num_floats) {
        if (texcoords == null) {
            texcoords = MemoryUtil.memAllocFloat(num_floats);
            return texcoords;
        }
        if (texcoords.capacity() < num_floats) {
            MemoryUtil.memFree(texcoords);
            texcoords = MemoryUtil.memAllocFloat(num_floats);
            return texcoords;
        }
        texcoords.limit(num_floats);
        return texcoords;
    }

    public FloatBuffer reserveNormalFloats(int num_floats) {
        if (normals == null) {
            normals = MemoryUtil.memAllocFloat(num_floats);
            return normals;
        }
        if (normals.capacity() < num_floats) {
            MemoryUtil.memFree(normals);
            normals = MemoryUtil.memAllocFloat(num_floats);
            return normals;
        }
        normals.limit(num_floats);
        return normals;
    }

    public void destroy() {
        if (vertices != null) {
            MemoryUtil.memFree(vertices);
            vertices = null;
        }
        if (texcoords != null) {
            MemoryUtil.memFree(texcoords);
            texcoords = null;
        }
        if (normals != null) {
            MemoryUtil.memFree(normals);
            normals = null;
        }
    }
}
