package org.theosib.GraphicsEngine;

import org.theosib.Adaptors.Disposable;
import org.theosib.Position.BlockPos;
import org.joml.Vector3dc;
import org.lwjgl.opengl.GL33;

public class MeshRenderer implements Disposable {
    int VAO;
    Texture tex;
    RenderDataBuffer vertex_data, texcoord_data, normal_data;
    GLArrayBuffer vertex_glarray, texcoord_glarray, normal_glarray;
    int total_vertices;
    BlockPos viewCenter;
    Vector3dc transSortPosition;
    boolean needs_gl_load = false;

    public boolean isValid() {
        return total_vertices > 0;
    }

    public void clear() {
        needs_gl_load = false;
        total_vertices = 0;
    }

    public void reset(Texture tex) {
        clear();
        this.tex = tex;
    }

    public Texture getTexture() {
        return tex;
    }

//    public void setViewCenter(BlockPos pos) {
//        viewCenter = pos;
//    }

    public BlockPos getViewCenter() {
        return viewCenter;
    }

    public void setTransSortPosition(Vector3dc pos) {
        transSortPosition = pos;
    }

    public Vector3dc getTransSortPosition() {
        return transSortPosition;
    }

    public void loadMeshes(Mesh[] meshes, Object[] positions, int[] faces, int count, BlockPos viewCenter) {
        this.viewCenter = viewCenter;
        System.out.println("Loading meshes " + count);

        total_vertices = 0;
        var total_vertex_floats = 0;
        var total_texcoord_floats = 0;
        var total_normal_floats = 0;

        for (int i=0; i<count; i++) {
            Mesh m = meshes[i];
            int f = faces[i];
            total_vertices += m.computeTriangleVertices(f);
            total_vertex_floats += m.computeVertexFloats(f);
            total_texcoord_floats += m.computeTextureFloats(f);
            total_normal_floats += m.computeNormalFloats(f);
        }

        vertex_data.reserveFloats(total_vertex_floats);
        texcoord_data.reserveFloats(total_texcoord_floats);
        normal_data.reserveFloats(total_normal_floats);

        for (int i=0; i<count; i++) {
            Mesh m = meshes[i];
            int f = faces[i];
            Object pi = positions[i];
            // XXX check type of just index 0?
            Vector3dc p = (pi instanceof BlockPos) ? ((BlockPos)pi).toVector3d() : (Vector3dc)pi;
            m.getVertexFloats(vertex_data.getBuffer(), f, p, viewCenter);
            m.getTexcoordFloats(texcoord_data.getBuffer(), f);
            m.getNormalFloats(normal_data.getBuffer(), f);
        }

        vertex_data.flip();
        texcoord_data.flip();
        normal_data.flip();

        needs_gl_load = true;
    }

    public MeshRenderer(Texture t) {
        tex = t;
        vertex_glarray = new GLArrayBuffer(0, 3);
        texcoord_glarray = new GLArrayBuffer(2, 2);
        normal_glarray = new GLArrayBuffer(1, 3);

        vertex_data = new RenderDataBuffer();
        texcoord_data = new RenderDataBuffer();
        normal_data = new RenderDataBuffer();
    }

    @Override
    public void destroy() {
        vertex_data.destroy();
        texcoord_data.destroy();
        normal_data.destroy();
        vertex_glarray.destroy();
        texcoord_glarray.destroy();
        normal_glarray.destroy();

        if (VAO != 0) {
            GL33.glDeleteVertexArrays(VAO);
            VAO = 0;
        }
    }

    private void bindVAO() {
        if (VAO == 0) VAO = GL33.glGenVertexArrays();
        GL33.glBindVertexArray(VAO);
    }

    private void unbindVAO() {
        GL33.glBindVertexArray(0);
    }

    public void loadGLBuffers() {
        if (total_vertices == 0) return;

        bindVAO();
        if (!needs_gl_load) return;
        needs_gl_load = false;
        System.out.println("Loading GL buffers");

        vertex_glarray.load(VAO, vertex_data.getBuffer());
        texcoord_glarray.load(VAO, texcoord_data.getBuffer());
        normal_glarray.load(VAO, normal_data.getBuffer());

//        for (int i=0; i<vertex_data.getBuffer().limit(); i++) {
//            System.out.printf("%f ", vertex_data.getBuffer().get(i));
//        }
//        System.out.println();
    }

    public void draw(Shader shader) {
        if (total_vertices == 0) return;

//        System.out.println("Drawing " + total_vertices + " vertices");
        loadGLBuffers();
        shader.bind();
        tex.bind();

        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, total_vertices);

        shader.unbind();
        tex.unbind();
        unbindVAO();
    }
}
