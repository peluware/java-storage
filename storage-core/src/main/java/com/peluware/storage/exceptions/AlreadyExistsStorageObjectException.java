package com.peluware.storage.exceptions;


import com.peluware.storage.StorageObjectRef;

public class AlreadyExistsStorageObjectException extends StorageException {

    private final StorageObjectRef ref;

    public AlreadyExistsStorageObjectException(StorageObjectRef ref) {
        super("Object already exists in " + ref.getPath());
        this.ref = ref;
    }

    public StorageObjectRef getRef() {
        return this.ref;
    }
}
