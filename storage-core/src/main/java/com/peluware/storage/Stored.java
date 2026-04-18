package com.peluware.storage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;

/**
 * Representa un archivo almacenado con su metadata. El contenido se descarga únicamente
 * al invocar {@link #openContent()}.
 */
@EqualsAndHashCode
@ToString
public class Stored {

    @Getter
    private final String directory;

    @Getter
    private final String fileName;

    @Getter
    private final String contentType;

    @Getter
    private final long fileSize;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final StorageContentLoader loader;

    public Stored(String directory, String fileName, String contentType, long fileSize, StorageContentLoader loader) {
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

    Stored withDirectory(String directory) {
        return new Stored(directory, this.fileName, this.contentType, this.fileSize, this.loader);
    }
}
