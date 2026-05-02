package com.peluware.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Representa un archivo almacenado con su metadata. El contenido se descarga únicamente
 * al invocar {@link #openContent()}.
 */
public class StoredObject {

    private final String directory;

    private final String fileName;

    private final String contentType;

    private final long fileSize;

    private final StorageContentLoader loader;

    public StoredObject(String directory, String fileName, String contentType, long fileSize, StorageContentLoader loader) {
        this.directory = directory;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.loader = loader;
    }

    public String getPath() {
        return StorageUtils.buildPath(directory, fileName);
    }

    public InputStream openContent() throws IOException {
        return loader.load();
    }

    public StoredObject withDirectory(String directory) {
        return new StoredObject(directory, this.fileName, this.contentType, this.fileSize, this.loader);
    }

    public String toString() {
        return "StoredObject(directory=" + this.directory + ", fileName=" + this.fileName + ", contentType=" + this.contentType + ", fileSize=" + this.fileSize + ")";
    }

    public String getDirectory() {
        return this.directory;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getContentType() {
        return this.contentType;
    }

    public long getFileSize() {
        return this.fileSize;
    }
}
