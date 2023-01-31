package org.theosib.GraphicsEngine;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.nio.FloatBuffer;

public interface Face {
    default public void setVertex(int index, float x, float y, float z) {
        setVertex(index, new Vector3f(x, y, z));
    }

    public void setVertex(int index, Vector3f point);

    public Vector3fc getVertex(int index);

    default public void setTexCoord(int index, float x, float y) {
        setTexCoord(index, new Vector2f(x, y));
    }

    public void setTexCoord(int index, Vector2f point);

    public Vector2fc getTexCoord(int index);

    public Vector3fc getNormal();

    public int numVertices();

    public int numTriangleVertices();

    default public int numVertexFloats() {
        return numTriangleVertices() * 3;
    }

    default public int numTextureFloats() {
        return numTriangleVertices() * 2;
    }

    default public int numNormalFloats() {
        return numVertexFloats();
    }

    default public void getVertexFloats(FloatBuffer outBuf, float offsetX, float offsetY, float offsetZ) {
        int loops = numTriangleVertices();
        for (int i = 0; i < loops; i++) {
            Vector3fc vertex = getVertex(i);
            outBuf.put(vertex.x() + offsetX);
            outBuf.put(vertex.y() + offsetY);
            outBuf.put(vertex.z() + offsetZ);
        }
    }

    default public void getTextcoordFloats(FloatBuffer outBuf) {
        int loops = numTriangleVertices();
        for (int i = 0; i < loops; i++) {
            Vector2fc tex = getTexCoord(i);
            outBuf.put(tex.x());
            outBuf.put(tex.y());
        }
    }

    default public void getNormalFloats(FloatBuffer outBuf) {
        int loops = numTriangleVertices();
        Vector3fc normal = getNormal();
        for (int i = 0; i < loops; i++) {
            outBuf.put(normal.x());
            outBuf.put(normal.y());
            outBuf.put(normal.z());
        }
    }

    static public class Triangle implements Face {
        private Vector3f[] vertices = new Vector3f[3];
        private Vector2f[] texcoords = new Vector2f[3];
        private Vector3f normal = null;

        @Override
        public void setVertex(int index, Vector3f point) {
            vertices[index] = point;
            normal = null;
        }

        @Override
        public Vector3fc getVertex(int index) {
            return vertices[index];
        }

        @Override
        public void setTexCoord(int index, Vector2f point) {
            texcoords[index] = point;
        }

        @Override
        public Vector2fc getTexCoord(int index) {
            return texcoords[index];
        }

        @Override
        public Vector3fc getNormal() {
            if (normal != null) return normal;

            Vector3f a = vertices[0];
            Vector3f b = vertices[1];
            Vector3f c = vertices[2];
            Vector3f cb = new Vector3f();
            Vector3f ab = new Vector3f();
            Vector3f x = new Vector3f();
            c.sub(b, cb);
            a.sub(b, ab);
            cb.cross(ab, x);
            normal = x.normalize();

            return normal;
        }

        @Override
        public int numVertices() {
            return 3;
        }

        @Override
        public int numTriangleVertices() {
            return 3;
        }
    }

    static public class Quad implements Face {
        private Vector3f[] vertices = new Vector3f[4];
        private Vector2f[] texcoords = new Vector2f[4];
        private Vector3f normal = null;
        private boolean solid = true;

        static private int[] repeat_indices = {
                0, 1, 2, 0, 2, 3
        };

        @Override
        public void setVertex(int index, Vector3f point) {
            vertices[index] = point;
            normal = null;
        }

        @Override
        public Vector3fc getVertex(int index) {
            return vertices[repeat_indices[index]];
        }

        @Override
        public void setTexCoord(int index, Vector2f point) {
            texcoords[index] = point;
        }

        @Override
        public Vector2fc getTexCoord(int index) {
            return texcoords[repeat_indices[index]];
        }

        @Override
        public Vector3fc getNormal() {
            if (normal != null) return normal;

            Vector3f a;
            Vector3f b;
            Vector3f c;

            if (vertices[0].equals(vertices[1]) || vertices[1].equals(vertices[2])) {
                a = vertices[0];
                b = vertices[2];
                c = vertices[3];
            } else {
                a = vertices[0];
                b = vertices[1];
                c = vertices[2];
            }

            Vector3f cb = new Vector3f();
            Vector3f ab = new Vector3f();
            Vector3f x = new Vector3f();
            c.sub(b, cb);
            a.sub(b, ab);
            cb.cross(ab, x);
            normal = x.normalize();

            return normal;
        }

        @Override
        public int numVertices() {
            return 4;
        }

        @Override
        public int numTriangleVertices() {
            return 6;
        }
    }

    static public class NoFace implements Face {
        public static final NoFace singleton = new NoFace();

        static final private Vector3f dummy3f = new Vector3f();
        static final private Vector2f dummy2f = new Vector2f();

        @Override
        public void setVertex(int index, Vector3f point) {
        }

        @Override
        public Vector3fc getVertex(int index) {
            return dummy3f;
        }

        @Override
        public void setTexCoord(int index, Vector2f point) {
        }

        @Override
        public Vector2fc getTexCoord(int index) {
            return dummy2f;
        }

        @Override
        public Vector3f getNormal() {
            return dummy3f;
        }

        @Override
        public int numVertices() {
            return 0;
        }

        @Override
        public int numTriangleVertices() {
            return 0;
        }
    }
}