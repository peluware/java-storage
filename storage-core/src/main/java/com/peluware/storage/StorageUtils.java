package com.peluware.storage;

import org.apache.tika.Tika;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;


public final class StorageUtils {

    private static final Tika TIKA = new Tika();

    private StorageUtils() {
        throw new UnsupportedOperationException("utility class");
    }

    public static String normalizeDirectory(String directory) {
        if (directory.isBlank() || directory.equals("/")) return "";
        var normalizedDir = directory.startsWith("/") ? directory.substring(1) : directory;
        return normalizedDir.endsWith("/") ? normalizedDir.substring(0, normalizedDir.length() - 1) : normalizedDir;
    }

    public static String buildPath(String path, String filename) {
        return path.isEmpty() ? filename : path + "/" + filename;
    }

    public static StoredObject newStoredObject(StorageContentLoader loader, long fileSize, String filename, String directory, String contentType) {
        return new StoredObject(directory, filename, contentType, fileSize, loader);
    }

    public static StoredObject newStoredObject(StorageContentLoader loader, long fileSize, String filename, String directory) {
        return newStoredObject(
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

    public static String detectContentType(byte[] prefix) {
        return TIKA.detect(prefix);
    }

    public static String detectContentType(InputStream header, String filename) throws IOException {
        return TIKA.detect(header, filename);
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

    public static void removeQuietly(Storage storage, @Nullable String path) {
        if (path == null) return;
        try {
            storage.remove(path);
        } catch (IOException ignored) {
        }
    }

    public static void removeQuietly(Storage storage, @Nullable String @Nullable ... paths) {
        if (paths == null) return;
        for (var path : paths) {
            removeQuietly(storage, path);
        }
    }
}
