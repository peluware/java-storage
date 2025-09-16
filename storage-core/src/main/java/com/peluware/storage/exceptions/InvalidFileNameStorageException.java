package com.peluware.storage.exceptions;

import lombok.Getter;

@Getter
public class InvalidFileNameStorageException extends StorageException {

    private final String filename;

    public InvalidFileNameStorageException(String filename, String reason) {
        super("Invalid filename: " + filename + " because " + reason);
        this.filename = filename;
    }
}
