package com.peluware.storage.exceptions;


import com.peluware.storage.StorageObjectRef;
import lombok.Getter;

@Getter
public class AlreadyFileExistsStorageException extends StorageException {

    private final String filename;
    private final String directory;

    private AlreadyFileExistsStorageException(String filename, String directory) {
        super("File already exists: " + filename + " in " + (directory.isEmpty() ? "root path" : directory));
        this.filename = filename;
        this.directory = directory;
    }

    public AlreadyFileExistsStorageException(StorageObjectRef pathFile) {
        this(pathFile.getFileName(), pathFile.getDirectory());
    }
}
