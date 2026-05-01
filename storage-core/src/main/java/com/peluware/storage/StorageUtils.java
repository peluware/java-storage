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

    public static StoredObject constructStoredFile(StorageContentLoader loader, long fileSize, String filename, String directory, String contentType) {
        return new StoredObject(directory, filename, contentType, fileSize, loader);
    }

    public static StoredObject constructStoredFile(StorageContentLoader loader, long fileSize, String filename, String directory) {
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

    public static String extractDirectory(String path) {
        return path.contains("/") ? path.substring(0, path.lastIndexOf("/") + 1) : "/";
    }

    public static String extractFilename(String path) {
        return path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
    }

    public static String extractExtension(String filename) {
        var dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
