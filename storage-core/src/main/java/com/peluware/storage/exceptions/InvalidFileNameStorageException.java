package com.peluware.storage.exceptions;

public class InvalidFileNameStorageException extends StorageException {

    private final String filename;

    public InvalidFileNameStorageException(String filename, String reason) {
        super("Invalid filename: " + filename + " because " + reason);
        this.filename = filename;
    }

    public String getFilename() {
        return this.filename;
    }
}
