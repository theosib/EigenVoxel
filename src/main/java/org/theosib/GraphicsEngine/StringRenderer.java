package org.theosib.GraphicsEngine;

import org.joml.Vector3dc;
import org.lwjgl.opengl.GL33;
import org.lwjgl.stb.STBTruetype;
import org.theosib.Adaptors.Disposable;
import org.theosib.Adaptors.RenderAgent;
import org.theosib.Adaptors.Window;
import org.theosib.Position.BlockPos;
import org.theosib.Utils.WindowDimensions;

import java.nio.FloatBuffer;

public class StringRenderer implements Disposable {
    FontAtlas fontAtlas;
    int VAO;
    volatile boolean needs_gl_load = false;
    volatile int total_vertices = 0;

    private RenderDataBuffer vertex_data, texcoord_data;
    private GLArrayBuffer vertex_glarray, texcoord_glarray;

    public StringRenderer(FontAtlas fontAtlas) {
        this.fontAtlas = fontAtlas;
        vertex_glarray = new GLArrayBuffer(0, 3);
        texcoord_glarray = new GLArrayBuffer(1, 2);
        vertex_data = new RenderDataBuffer();
        texcoord_data = new RenderDataBuffer();
    }


    static final private int[] repeat_indices = {
            0, 1, 2, 0, 2, 3
    };

    private final static double[] vertexX = new double[4];
    private final static double[] vertexY = new double[4];
    private final static double[] vertexZ = new double[4];
    private final static double[] texX = new double[4];
    private final static double[] texY = new double[4];

    private static void loadVertexFloats(double[] vertexX, double[] vertexY, double[] vertexZ, FloatBuffer buf) {
        for (int j=0; j<6; j++) {
            int i = repeat_indices[j];
            System.out.println("Vertex: " + vertexX[i] + " " + vertexY[i] + " " + vertexZ[i]);
            buf.put((float)vertexX[i]);
            buf.put((float)vertexY[i]);
            buf.put((float)vertexZ[i]);
        }
    }

    private static void loadTextureFloats(double[] texX, double[] texY, FloatBuffer buf) {
        for (int j=0; j<6; j++) {
            int i = repeat_indices[j];
            System.out.println("Texture: " + texX[i] + " " + texY[i]);
            buf.put((float)texX[i]);
            buf.put((float)texY[i]);
        }
    }

    public void loadMesh(String string, Vector3dc corner, Vector3dc right, Vector3dc down, double hscale, double vscale, BlockPos viewCenter) {
        final int numChar = string.length();
        final int numVertices = 6 * numChar;
        final int total_vertex_floats = numVertices * 3;
        final int total_texcoord_floats = numVertices * 2;

        vertex_data.reserveFloats(total_vertex_floats);
        texcoord_data.reserveFloats(total_texcoord_floats);

        int texWidth = fontAtlas.getAtlasWidth();
        int texHeight = fontAtlas.getAtlasHeight();
        int ascent = fontAtlas.getAscent();

        double centerX = viewCenter != null ? viewCenter.X() : 0;
        double centerY = viewCenter != null ? viewCenter.Y() : 0;
        double centerZ = viewCenter != null ? viewCenter.Z() : 0;

        int x = 0;
        for (int ix = 0; ix < numChar; ix++) {
            char c = string.charAt(ix);
            FontAtlas.CharInfo ci = fontAtlas.getCharInfo(c);

            if (ix == 0) {
                x -= ci.leftSideBearing;
            }
            int px = x + ci.leftSideBearing;
            int py = ascent + ci.shift_y;

            vertexX[0] = corner.x() + right.x() * hscale * px + down.x() * vscale * py - centerX;
            vertexY[0] = corner.y() + right.y() * hscale * px + down.y() * vscale * py - centerY;
            vertexZ[0] = corner.z() + right.z() * hscale * px + down.z() * vscale * py - centerZ;

            vertexX[1] = vertexX[0] + down.x() * vscale * ci.bitmap_h;
            vertexY[1] = vertexY[0] + down.y() * vscale * ci.bitmap_h;
            vertexZ[1] = vertexZ[0] + down.z() * vscale * ci.bitmap_h;

            vertexX[2] = vertexX[1] + right.x() * hscale * ci.bitmap_w;
            vertexY[2] = vertexY[1] + right.y() * hscale * ci.bitmap_w;
            vertexZ[2] = vertexZ[1] + right.z() * hscale * ci.bitmap_w;

            vertexX[3] = vertexX[0] + right.x() * hscale * ci.bitmap_w;
            vertexY[3] = vertexY[0] + right.y() * hscale * ci.bitmap_w;
            vertexZ[3] = vertexZ[0] + right.z() * hscale * ci.bitmap_w;

            loadVertexFloats(vertexX, vertexY, vertexZ, vertex_data.getBuffer());

            texX[0] = (double)(ci.bitmap_x) / (double)texWidth;
            texY[0] = (double)(ci.bitmap_y) / (double)texHeight;
            texX[1] = texX[0];
            texY[1] = (double)(ci.bitmap_y + ci.bitmap_h) / (double)texHeight;
            texX[2] = (double)(ci.bitmap_x + ci.bitmap_w) / (double)texWidth;
            texY[2] = texY[1];
            texX[3] = texX[2];
            texY[3] = texY[0];

            loadTextureFloats(texX, texY, texcoord_data.getBuffer());

            x += ci.advanceWidth;
            if (ix < numChar - 1) {
                char d = string.charAt(ix + 1);
                x += fontAtlas.getKerning(c, d);
            }
        }

        vertex_data.flip();
        texcoord_data.flip();

        needs_gl_load = true;
        total_vertices = numVertices;
    }

    private void bindVAO() {
        if (VAO == 0) VAO = GL33.glGenVertexArrays();
//        System.out.println("Bind VAO: " + VAO);
        GL33.glBindVertexArray(VAO);
    }

    private void unbindVAO() {
        GL33.glBindVertexArray(0);
    }

    private void loadGLBuffers() {
        if (total_vertices == 0) return;

        bindVAO();
        if (!needs_gl_load) return;
        needs_gl_load = false;

        vertex_glarray.load(VAO, vertex_data.getBuffer());
        texcoord_glarray.load(VAO, texcoord_data.getBuffer());
    }

    public void draw(Shader shader) {
        if (total_vertices == 0) return;

        loadGLBuffers();
        shader.bind();
        fontAtlas.bind();
        RenderingUtils.enableBlend();

//        System.out.println("Vertices: " + total_vertices);
        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, total_vertices);

        RenderingUtils.disableBlend();
        shader.unbind();
        fontAtlas.unbind();
        unbindVAO();
    }

    @Override
    public void destroy() {
        vertex_data.destroy();
        texcoord_data.destroy();
        vertex_glarray.destroy();
        texcoord_glarray.destroy();

        if (VAO != 0) {
            GL33.glDeleteVertexArrays(VAO);
            VAO = 0;
        }
    }
}
