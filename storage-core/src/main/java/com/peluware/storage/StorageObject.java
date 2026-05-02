package com.peluware.storage;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class StorageObject extends StorageObjectRef {

    private final InputStream content;
    private final @Nullable Long contentLength;

    public StorageObject(String directory, String filename, InputStream content, @Nullable Long contentLength) {
        super(directory, filename);
        this.content = content;
        this.contentLength = contentLength;
    }

    public StorageObject(String directory, String filename, InputStream content) {
        this(directory, filename, content, null);
    }

    public StorageObject(String filename, InputStream content) {
        this("", filename, content, null);
    }

    public StorageObject(String directory, String filename, byte[] content) {
        this(directory, filename, new ByteArrayInputStream(content), (long) content.length);
    }

    public StorageObject(String filename, byte[] content) {
        this("", filename, content);
    }

    public InputStream getContent() {
        return this.content;
    }

    public @Nullable Long getContentLength() {
        return this.contentLength;
    }
}
