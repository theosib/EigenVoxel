package org.theosib.GraphicsEngine;

import org.theosib.Geometry.AxisAlignedBox;
import org.theosib.Geometry.CollisionShape;
import org.theosib.Parser.ConfigParser;
import org.theosib.Position.BlockPos;
import org.theosib.Utils.Facing;
import org.theosib.Utils.FileLocator;
import org.joml.Vector3dc;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Mesh {
    Texture texture;
    List<Face> faces = new ArrayList<>();
    CollisionShape collision = new CollisionShape();
    boolean translucent = false;
    int solidFaces;

    public Mesh() {
        System.out.println("Creating Mesh");
    }

    public void setTexture(Texture t) {
        texture = t;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTranslucent(boolean trans) {
        translucent = trans;
    }

    public boolean isTranslucent() {
        return translucent;
    }

    public int numFaces() {
        return faces.size();
    }

    public int getSolidFaces() {
        return solidFaces;
    }

    public void setSolidFaces(int sf) {
        solidFaces = sf;
    }

    public CollisionShape getCollision() {
        return collision;
    }

    public void setCollision(CollisionShape collision) {
        this.collision = collision;
    }

    public Face getFace(int index) {
        if (index >= faces.size()) return Face.NoFace.singleton;
        return faces.get(index);
    }

    public void addFace(Face face) {
        faces.add(face);
    }

    public Face allocateFace(int index, Class faceClass) {
        if (index >= faces.size()) {
            while (faces.size() < index) {
                faces.add(Face.NoFace.singleton);
            }
            try {
                faces.add((Face)faceClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e); // XXX
            }
            return faces.get(index);
        }
        Face face = faces.get(index);
        if (face == Face.NoFace.singleton) {
            try {
                faces.set(index, (Face)faceClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e); // XXX
            }
            return faces.get(index);
        }
        if (face.getClass() != faceClass) {
            throw new RuntimeException("Mismatch on face type for index " + index); // XXX
        }
        return face;
    }

    public void setFace(int index, Face face) {
        while (faces.size() <= index) {
            faces.add(Face.NoFace.singleton);
        }
        faces.set(index, face);
    }

    public boolean faceIsSolid(int index) {
        return Facing.hasFace(solidFaces, index);
    }

    public int computeTriangleVertices(int show_faces) {
        int total = 0;
        for (int facenum=0; facenum<faces.size(); facenum++) {
            if (facenum >= Facing.NUM_FACES() || Facing.hasFace(show_faces, facenum)) {
                total += getFace(facenum).numTriangleVertices();
            }
        }
        return total;
    }

    public int computeVertexFloats(int show_faces) {
        int total = 0;
        for (int facenum=0; facenum<faces.size(); facenum++) {
            if (facenum >= Facing.NUM_FACES() || Facing.hasFace(show_faces, facenum)) {
                total += getFace(facenum).numVertexFloats();
            }
        }
        return total;
    }

    public int computeTextureFloats(int show_faces) {
        int total = 0;
        for (int facenum=0; facenum<faces.size(); facenum++) {
            if (facenum >= Facing.NUM_FACES() || Facing.hasFace(show_faces, facenum)) {
                total += getFace(facenum).numTextureFloats();
            }
        }
        return total;
    }

    public int computeNormalFloats(int show_faces) {
        int total = 0;
        for (int facenum=0; facenum<faces.size(); facenum++) {
            if (facenum >= Facing.NUM_FACES() || Facing.hasFace(show_faces, facenum)) {
                total += getFace(facenum).numNormalFloats();
            }
        }
        return total;
    }

    public void getVertexFloats(FloatBuffer outBuf, int show_faces, Vector3dc blockPos, BlockPos viewCenter) {
        float offsetX = (float)(blockPos.x() - viewCenter.X());
        float offsetY = (float)(blockPos.y() - viewCenter.Y());
        float offsetZ = (float)(blockPos.z() - viewCenter.Z());

        for (int facenum=0; facenum<faces.size(); facenum++) {
            if (facenum >= Facing.NUM_FACES() || Facing.hasFace(show_faces, facenum)) {
                getFace(facenum).getVertexFloats(outBuf, offsetX, offsetY, offsetZ);
            }
        }
    }

    public void getTexcoordFloats(FloatBuffer outBuf, int show_faces) {
        for (int facenum=0; facenum<faces.size(); facenum++) {
            if (facenum >= Facing.NUM_FACES() || Facing.hasFace(show_faces, facenum)) {
                getFace(facenum).getTextcoordFloats(outBuf);
            }
        }
    }

    public void getNormalFloats(FloatBuffer outBuf, int show_faces) {
        for (int facenum=0; facenum<faces.size(); facenum++) {
            if (facenum >= Facing.NUM_FACES() || Facing.hasFace(show_faces, facenum)) {
                getFace(facenum).getNormalFloats(outBuf);
            }
        }
    }

    private float[] parseCoords(String coords) {
        String[] coords_s = coords.split("\\s+");
        float[] coords_f = new float[coords_s.length];
        for (int i=0; i<coords_s.length; i++) {
            coords_f[i] = Float.parseFloat(coords_s[i]);
        }
        return coords_f;
    }

    private void parseFaceVertices(int face_num, String all_coords) {
        float[] coords = parseCoords(all_coords);

        Face face = null;
        if (coords.length == 9) {
            face = allocateFace(face_num, Face.Triangle.class);
        } else if (coords.length == 12) {
            face = allocateFace(face_num, Face.Quad.class);
        } else {
            throw new RuntimeException("Wrong number of vertex coordinates: " + coords.length);
        }

        for (int i=0, j=0; i<coords.length; i+=3, j++) {
            float x = coords[i];
            float y = coords[i+1];
            float z = coords[i+2];
            face.setVertex(j, x, y, z);
        }
    }

    private void parseFaceTexture(int face_num, String all_coords, boolean int_tex_coords, Texture tex) {
        float[] coords = parseCoords(all_coords);

        Face face = null;
        if (coords.length == 6) {
            face = allocateFace(face_num, Face.Triangle.class);
        } else if (coords.length == 8) {
            face = allocateFace(face_num, Face.Quad.class);
        } else {
            throw new RuntimeException("Wrong number of texture coordinates: " + coords.length);
        }

        for (int i=0, j=0; i<coords.length; i+=2, j++) {
            float x = coords[i];
            float y = coords[i+1];
            if (int_tex_coords) {
                x /= tex.getWidth();
                y /= tex.getHeight();
            }
            face.setTexCoord(j, x, y);
        }
    }

    private void parseCollisionBox(String all_coords) {
        float[] coords = parseCoords(all_coords);

        if (collision == null) {
            collision = new CollisionShape();
        }

        for (int i=0; i<coords.length; i+=6) {
            float x1 = coords[i];
            float y1 = coords[i+1];
            float z1 = coords[i+2];
            float x2 = coords[i+3];
            float y2 = coords[i+4];
            float z2 = coords[i+5];
            collision.append(new AxisAlignedBox(x1, y1, z1, x2, y2, z2));
        }
    }

    private void setSolidFaces(String list) {
        String[] face_list = list.split("\\s+");
        solidFaces = 0;
        for (String f : face_list) {
            if (f.charAt(0) == 'a') {
                solidFaces = -1;
                break;
            } else {
                int face_num = Facing.faceFromName(f);
                solidFaces |= Facing.bitMask(face_num, 1);
            }
        }
    }
    public void loadMesh(String name) {
        boolean int_tex_coords = false;
        Texture tex = null;

        LinkedHashMap<String,String> props = ConfigParser.parse(FileLocator.FileCategory.Blocks, name);
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();

            System.out.println("Key: " + key + " Val: " + val);

            if (key.startsWith("tex:")) {
                int face_num = Facing.faceFromName(key.split(":")[1]);
                parseFaceTexture(face_num, val, int_tex_coords, tex);
            } else if (key.startsWith("face:")) {
                int face_num = Facing.faceFromName(key.split(":")[1]);
                parseFaceVertices(face_num, val);
            } else if (key.startsWith("texture")) {
                tex = Texture.lookupTexture(val);
                System.out.println("Setting texture " + tex + " for mesh " + this);
                setTexture(tex);
            } else if (key.startsWith("tex-scale")) {
                if (val.charAt(0) == 'f') {
                    int_tex_coords = false;
                } else if (val.charAt(0) == 'i') {
                    int_tex_coords = true;
                }
            } else if (key.startsWith("translucent")) {
                if (val.charAt(0) == 't') setTranslucent(true);
            } else if (key.startsWith("solid-faces")) {
                setSolidFaces(val);
            } else if (key.startsWith("box")) {
                parseCollisionBox(val);
            } else {
                throw new RuntimeException("Unknown key: " + key);
            }
        }
    }
}
