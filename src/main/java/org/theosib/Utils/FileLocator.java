package org.theosib.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileLocator {
    public static enum FileCategory {
        Textures("textures"),
        Blocks("blocks"),
        Shader("shaders"),
        Chunk("chunks"),
        Config("config");

        public final String subdir;

        private FileCategory(String s) {
            this.subdir = s;
        }
    }

    static String baseDir = null;
    public static void setBaseDir(String bd) {
        baseDir = bd;
    }

    public static String computePath(FileCategory category, String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseDir).append(File.separator).append(category.subdir);
        if (filename == null) return sb.toString();
        return sb.append(File.separator).append(filename).toString();
    }

    public static String getFileAsString(FileCategory category, String filename) throws IOException {
        String path = computePath(category, filename);
        return Files.readString(Path.of(path));
    }
}
