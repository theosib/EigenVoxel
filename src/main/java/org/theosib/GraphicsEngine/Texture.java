package org.theosib.GraphicsEngine;


import org.theosib.Adaptors.Disposable;
import org.theosib.Files.Image;
import org.theosib.Utils.FileLocator;
import org.lwjgl.opengl.GL33;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Texture implements Disposable {
    private static Map<String,Texture> library = new HashMap<>();
    private static ArrayList<Texture> texArray = new ArrayList<>();

    public static Texture lookupTexture(String name) {
        System.out.println("Looking up texture: " + name);
        Texture tex = library.get(name);
        if (tex != null) return tex;

        int index = texArray.size();
        tex = new Texture(name, index);
        library.put(name, tex);
        texArray.add(tex);
        return tex;
    }

    String name;
    boolean translucent;
    private int index;

    @Override
    public String toString() {
        return "texture(" + name + ")";
    }

    public int getIndex() {
        return index;
    }

    public static int numTexures() {
        return texArray.size();
    }

    private Texture(String name, int index) {
        this.name = name;
        this.index = index;
    }

    private int texID = 0;
    private int width = 0;
    private int height = 0;
    private boolean destroyed = false;

    public int getWidth() {
        if (width == 0) loadGLTexture(name);
        return width;
    }

    public int getHeight() {
        if (height == 0) loadGLTexture(name);
        return height;
    }

    public boolean isValid() {
        return !destroyed;
    }

    public static Texture getTexture(int index) {
        return texArray.get(index);
    }

    private void loadGLTexture(String name) {
        String fname = name + ".png";
        String path = FileLocator.computePath(FileLocator.FileCategory.Textures, fname);
        Image image = new Image(path);

        int texID = GL33.glGenTextures();
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, texID);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_LINEAR);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_EDGE);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_EDGE);

        int format;
        if (image.comp() == 3) {
            if ((image.w() & 3) != 0) {
                GL33.glPixelStorei(GL33.GL_UNPACK_ALIGNMENT, 2 - (image.w() & 1));
            }
            format = GL33.GL_RGB;
        } else {
            image.premultiplyAlpha();
            format = GL33.GL_RGBA;
        }

        GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, format, image.w(), image.h(), 0, format, GL33.GL_UNSIGNED_BYTE, image.imageBuf());
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, 0);

        this.translucent = image.comp() > 3;
        this.texID = texID;
        this.width = image.w();
        this.height = image.h();
        image.destroy();
    }

    void bind() {
        if (texID == 0) loadGLTexture(name);

        GL33.glBindTexture(GL33.GL_TEXTURE_2D, texID);
        if (translucent) GL33.glEnable(GL33.GL_BLEND);
        GL33.glEnable(GL33.GL_TEXTURE_2D);
        GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE_MINUS_SRC_ALPHA);
    }

    static void unbind() {
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, 0);
    }

    @Override
    public void destroy() {
        if (texID == 0) return;
        GL33.glDeleteTextures(texID);
        texID = 0;
        library.remove(name);
        texArray.set(index, null);
        destroyed = true;
    }
}
