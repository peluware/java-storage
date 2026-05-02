package com.peluware.storage.exceptions;

import com.peluware.storage.StorageObjectRef;

public class StorageObjectNotFoundException extends StorageException {

    private final StorageObjectRef ref;

    public StorageObjectNotFoundException(StorageObjectRef ref) {
        super("Object not found: " + ref.getFileName() + " in " + ref.getDirectory());
        this.ref = ref;
    }

    public StorageObjectRef getRef() {
        return this.ref;
    }
}