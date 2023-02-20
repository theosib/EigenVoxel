package org.theosib.GraphicsEngine;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.theosib.Utils.FileLocator;
import org.theosib.Utils.IOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class FontAtlas {
    final static int atlasWidth = 1024;

    ByteBuffer fontBuffer;
    private int texID;
    STBTTFontinfo info;
    private int ascent;
    private int descent;
    private int lineGap;
    private int lineHeight;
    private float scale;

    private int atlasHeight;


    int getAtlasHeight() {
        return atlasHeight;
    }

    int getAtlasWidth() {
        return atlasWidth;
    }

    int getAscent() {
        return ascent;
    }

    int getDescent() {
        return descent;
    }


    static ByteBuffer loadFontFile(String fontName) {
        String fname = FileLocator.computePath(FileLocator.FileCategory.Fonts, fontName);
        try {
            return IOUtil.ioResourceToByteBuffer(fname, 8 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static STBTTFontinfo initializeFontInfo(ByteBuffer fontBuffer) {
        STBTTFontinfo info = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(info, fontBuffer)) {
            throw new IllegalStateException("Failed to initialize font information.");
        }
        return info;
    }

    void getFontVMetrics(STBTTFontinfo info, int pointsize) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pAscent  = stack.mallocInt(1);
            IntBuffer pDescent = stack.mallocInt(1);
            IntBuffer pLineGap = stack.mallocInt(1);

            STBTruetype.stbtt_GetFontVMetrics(info, pAscent, pDescent, pLineGap);

            ascent = pAscent.get(0);
            descent = pDescent.get(0);
            lineGap = pLineGap.get(0);
        }

        scale = (float)pointsize / (float)ascent;
        ascent = (int)Math.round(ascent * scale);
        descent = (int)Math.round(descent * scale);
        lineGap = (int)Math.round(lineGap * scale);
        lineHeight = ascent - descent + lineGap;
    }

    class CharInfo {
        int bitmap_x, bitmap_y;
        int bitmap_w, bitmap_h;
        int advanceWidth;
        int leftSideBearing;
        int shift_x, shift_y;
        int glyph_index;
    }

    private Map<Integer,CharInfo> charInfoMap = new HashMap<>();

    CharInfo getCharInfo(int codepoint) {
        return charInfoMap.get(codepoint);
    }

    ByteBuffer makeAtlas(Map<Integer,CharInfo> charInfoMap) {
        atlasHeight = 0;

        // Gather a map of info for all glyphs and where they will appear in the atlas.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer leftSideBearing = stack.mallocInt(1);
            IntBuffer c_x1 = stack.mallocInt(1);
            IntBuffer c_y1 = stack.mallocInt(1);
            IntBuffer c_x2 = stack.mallocInt(1);
            IntBuffer c_y2 = stack.mallocInt(1);

            int x = 0;
            int y = 0;
            int max_height = 0;

            for (int codepoint=0; codepoint<Character.MAX_VALUE; codepoint++) {
                int glyph_index = STBTruetype.stbtt_FindGlyphIndex(info, codepoint);
                if (glyph_index == 0) continue;

                STBTruetype.stbtt_GetGlyphHMetrics(info, glyph_index, advanceWidth, leftSideBearing);
                STBTruetype.stbtt_GetGlyphBitmapBox(info, glyph_index, scale, scale, c_x1, c_y1, c_x2, c_y2);

                int box_x = c_x1.get(0);
                int box_y = c_y1.get(0);
                int box_width = c_x2.get(0) - box_x;
                int box_height = c_y2.get(0) - box_y;

                if (x + box_width > 1024) {
                    x = 0;
                    y += max_height + 1;
                    max_height = 0;
                }

                if (box_height > max_height) max_height = box_height;
                int lsb = (int)Math.round(leftSideBearing.get(0) * scale);
                int aw = (int)Math.round(advanceWidth.get(0) * scale);
                if (lsb != box_x) {
                    System.out.println("LSB doesn't match box_x");
                }

                CharInfo ci = new CharInfo();
                ci.bitmap_x = x;
                ci.bitmap_y = y;
                ci.bitmap_w = box_width;
                ci.bitmap_h = box_height;
                ci.shift_x = box_x;
                ci.shift_y = box_y;
                ci.leftSideBearing = lsb;
                ci.advanceWidth = aw;
                ci.glyph_index = glyph_index;
                charInfoMap.put(codepoint, ci);

                x += box_width + 1;
            }

            atlasHeight = y + max_height + 1;
        }

        ByteBuffer imageBuf = BufferUtils.createByteBuffer(atlasWidth * atlasHeight);

        // Render all glyphs to the bytebuffer
        for (Map.Entry<Integer,CharInfo> entry : charInfoMap.entrySet()) {
            int codepoint = entry.getKey();
            CharInfo ci = entry.getValue();
            int glyph_index = ci.glyph_index;

            int y = ci.bitmap_y;
            int x = ci.bitmap_x;
            int byteOffset = x + (y * atlasWidth);

            imageBuf.position(byteOffset);
            STBTruetype.stbtt_MakeGlyphBitmap(info, imageBuf, ci.bitmap_w, ci.bitmap_h, atlasWidth, scale, scale, glyph_index);
        }
        imageBuf.position(0);

//        STBImageWrite.stbi_write_png("atlas.png", atlasWidth, atlasHeight, 1, imageBuf, atlasWidth);

        return imageBuf;
    }

    int computeStringWidth(String str) {
        int width = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            int c = str.charAt(i);
            CharInfo ci = charInfoMap.get(c);

            if (i == 0) width -= ci.leftSideBearing;
            if (i == len-1) {
                width += ci.bitmap_w + ci.shift_x;
            } else {
                width += ci.advanceWidth;
                char d = str.charAt(i + 1);
                int kern = (int)Math.round(scale * STBTruetype.stbtt_GetCodepointKernAdvance(info, c, d));
                width += kern;
            }
        }
        return width;
    }

    void copyBytes(ByteBuffer src, ByteBuffer dst, int sx, int sy, int ss, int dx, int dy, int ds, int w, int h) {
        for (int y=0; y<h; y++) {
            for (int x=0; x<w; x++) {
                int sp = x + sx + (y + sy) * ss;
                int dp = x + dx + (y + dy) * ds;
                dst.put(dp, src.get(sp));
            }
        }
    }

    void renderString(String str, ByteBuffer atlasBuffer, ByteBuffer byteBuffer, int bw, int bh) {
        int x = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            CharInfo ci = charInfoMap.get((int)c);
            if (i == 0) {
                x -= ci.leftSideBearing;
            }
            int px = x + ci.leftSideBearing;
            int py = ascent + ci.shift_y;

            copyBytes(atlasBuffer, byteBuffer, ci.bitmap_x, ci.bitmap_y, atlasWidth, px, py, bw, ci.bitmap_w, ci.bitmap_h);

            x += ci.advanceWidth;

            if (i < len - 1) {
                char d = str.charAt(i + 1);
                int kern = (int)Math.round(scale * STBTruetype.stbtt_GetCodepointKernAdvance(info, c, d));
                x += kern;
            }
        }
    }

    public int getKerning(int cp0, int cp1) {
        return (int)Math.round(scale * STBTruetype.stbtt_GetCodepointKernAdvance(info, cp0, cp1));
    }

    public void bind() {
        if (texID != 0) {
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, texID);
            return;
        }

        texID = GL33.glGenTextures();
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, texID);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_LINEAR);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_EDGE);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_EDGE);
        GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RED, atlasWidth, atlasHeight, 0, GL33.GL_RED, GL33.GL_UNSIGNED_BYTE, fontBuffer);

        // Let the garbage collector get this buffer eventually
        fontBuffer = null;
    }

    public void unbind() {
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, 0); // Refactor these into RenderingUtils
    }

    /*int measureStringWidth(String str) {
        int x = 0;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer leftSideBearing = stack.mallocInt(1);
            IntBuffer c_x1 = stack.mallocInt(1);
            IntBuffer c_y1 = stack.mallocInt(1);
            IntBuffer c_x2 = stack.mallocInt(1);
            IntBuffer c_y2 = stack.mallocInt(1);

            int len = str.length();
            for (int i = 0; i < len; i++) {
                char c = str.charAt(i);
                char d = (i+1)<len ? str.charAt(i+1) : 0;

                STBTruetype.stbtt_GetCodepointHMetrics(info, c, advanceWidth, leftSideBearing);
                STBTruetype.stbtt_GetCodepointBitmapBox(info, c, scale, scale, c_x1, c_y1, c_x2, c_y2);

                int y = ascent + c_y1.get(0);
//                int byteOffset = x + Math.round(leftSideBearing.get(0) * scale) + (y * b_w);

                x += (int)Math.round(advanceWidth.get(0) * scale);

                int kern = STBTruetype.stbtt_GetCodepointKernAdvance(info, c, d);
                x += (int)Math.round(kern * scale);
            }
        }

        return x;
    }*/

    /*StringImageBuffer renderString(String str, int bw, int bh, int lpad) {
        ByteBuffer imageBuf = BufferUtils.createByteBuffer(bw * bh);

        int x = 0;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer leftSideBearing = stack.mallocInt(1);
            IntBuffer c_x1 = stack.mallocInt(1);
            IntBuffer c_y1 = stack.mallocInt(1);
            IntBuffer c_x2 = stack.mallocInt(1);
            IntBuffer c_y2 = stack.mallocInt(1);

            int len = str.length();
            for (int i = 0; i < len; i++) {
                char c = str.charAt(i);
                char d = (i+1)<len ? str.charAt(i+1) : 0;

                STBTruetype.stbtt_GetCodepointHMetrics(info, c, advanceWidth, leftSideBearing);
                STBTruetype.stbtt_GetCodepointBitmapBox(info, c, scale, scale, c_x1, c_y1, c_x2, c_y2);

                System.out.println();
                System.out.println("aw:" + (advanceWidth.get(0) * scale) + " lsb:" + (leftSideBearing.get(0) * scale));
                System.out.println("cx1:" + c_x1.get(0) + " cy1:" + c_y1.get(0) + " cx2:" + c_x2.get(0) + " cy2:" + c_y2.get(0));
                int y = ascent + c_y1.get(0);
                int byteOffset = x + Math.round(leftSideBearing.get(0) * scale) + (y * bw);

                // Set buffer position to get horizontal shift
                imageBuf.position(byteOffset + lpad);
                STBTruetype.stbtt_MakeCodepointBitmap(info, imageBuf,c_x2.get(0) - c_x1.get(0), c_y2.get(0) - c_y1.get(0), bw, scale, scale, c);

                x += (int)Math.round(advanceWidth.get(0) * scale);

                int kern = STBTruetype.stbtt_GetCodepointKernAdvance(info, c, d);
                System.out.println("kern:" + (kern * scale));
                x += (int)Math.round(kern * scale);
            }

            // Must reset buffer position to zero when finished
            imageBuf.position(0);
        }

        return new StringImageBuffer(imageBuf, bw, bw, bh);
    }*/


    public FontAtlas(String fontName, int pointsize) {
        fontBuffer = loadFontFile(fontName);
        info = initializeFontInfo(fontBuffer);
        getFontVMetrics(info, pointsize);
//        int w = measureStringWidth(str);
//        System.out.println("ascent=" + ascent + " descent=" + descent + " lineGap=" + lineGap + " scale=" + scale
//            + " w=" + w);

//        int bw = w + 6;
//        StringImageBuffer buf = renderString(str, bw, ascent - descent, 4);
//
//        STBImageWrite.stbi_write_png("out.png", bw, ascent-descent, 1, buf.getBuffer(), bw);

        fontBuffer = makeAtlas(charInfoMap);


        //void renderString(String str, ByteBuffer atlasBuffer, ByteBuffer byteBuffer, int bw, int bh) {

        /*int bw = computeStringWidth(str);
        System.out.println("bw=" + bw);
        ByteBuffer stringbuf = BufferUtils.createByteBuffer(bw * lineHeight);
        renderString(str, atlas, stringbuf, bw, lineHeight);
        STBImageWrite.stbi_write_png("out.png", bw, lineHeight, 1, stringbuf, bw);*/
    }

    static Map<String,FontAtlas> fontLibrary = new HashMap<>();

    static public void registerFont(String name, FontAtlas fontAtlas) {
        fontLibrary.put(name, fontAtlas);
    }

    static public FontAtlas lookupFont(String name) {
        return fontLibrary.get(name);
    }

    static {
        registerFont("default", new FontAtlas("PixeloidSans.ttf", 18));
        //registerFont("swansea", new FontAtlas("Swansea-q3pd.ttf", 18*2));
    }
}
