package com.peluware.storage.exceptions;

public class InvalidPathStorageException extends StorageException {

    private final String path;

    public InvalidPathStorageException(String path, String reason) {
        super("Invalid path: " + path + " because " + reason);
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
