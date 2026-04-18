package com.peluware.storage.exceptions;

import com.peluware.storage.StorageObjectRef;
import lombok.Getter;

@Getter
public class StorageNotFoundException extends StorageException {

    private final String fileName;
    private final String directory;

    private StorageNotFoundException(String fileName, String directory) {
        super("File not found: " + fileName + " in " + directory);
        this.fileName = fileName;
        this.directory = directory;
    }

    public StorageNotFoundException(StorageObjectRef ref) {
        this(ref.getFileName(), ref.getDirectory());
    }
}