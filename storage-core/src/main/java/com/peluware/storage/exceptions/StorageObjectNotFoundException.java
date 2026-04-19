package com.peluware.storage.exceptions;

import com.peluware.storage.StorageObjectRef;
import lombok.Getter;

@Getter
public class StorageObjectNotFoundException extends StorageException {

    private final StorageObjectRef ref;

    public StorageObjectNotFoundException(StorageObjectRef ref) {
        super("Object not found: " + ref.getFileName() + " in " + ref.getDirectory());
        this.ref = ref;
    }
}