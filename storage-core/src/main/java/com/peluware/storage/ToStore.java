package com.peluware.storage;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ToStore extends PathFile {

    private final InputStream stream;

    public ToStore(String path, String filename, InputStream stream) {
        super(path, filename);
        if (stream == null) {
            throw new IllegalArgumentException("Content is required");
        }
        this.stream = stream;
    }


    public ToStore(String filename, InputStream stream) {
        this("", filename, stream);
    }

    public ToStore(String path, String filename, byte[] content) {
        this(path, filename, new ByteArrayInputStream(content));
    }

    public ToStore(String filename, byte[] content) {
        this("", filename, content);
    }

}
