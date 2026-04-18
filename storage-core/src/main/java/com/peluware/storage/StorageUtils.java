package com.peluware.storage;

import lombok.experimental.UtilityClass;
import org.apache.tika.Tika;


@UtilityClass
public final class StorageUtils {

    private static final Tika TIKA = new Tika();

    public static String normalizeDirectory(String directory) {
        if (directory.isBlank() || directory.equals("/")) return "";
        var normalizedDir = directory.startsWith("/") ? directory.substring(1) : directory;
        return normalizedDir.endsWith("/") ? normalizedDir.substring(0, normalizedDir.length() - 1) : normalizedDir;
    }

    public static String buildPath(String path, String filename) {
        return path.isEmpty() ? filename : path + "/" + filename;
    }

    public static Stored constructStoredFile(StorageContentLoader loader, long fileSize, String filename, String directory, String contentType) {
        return new Stored(directory, filename, contentType, fileSize, loader);
    }

    public static Stored constructStoredFile(StorageContentLoader loader, long fileSize, String filename, String directory) {
        return constructStoredFile(
            loader,
            fileSize,
            filename,
            directory,
            guessContentType(filename)
        );
    }

    public static String guessContentType(String filename) {
        return TIKA.detect(filename);
    }
}
