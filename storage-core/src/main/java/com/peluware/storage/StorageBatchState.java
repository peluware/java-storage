package com.peluware.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class StorageBatchState {
    private final List<StorageObject> storageObjects;
    private final List<String> toRemove;

    public StorageBatchState() {
        this.storageObjects = new ArrayList<>();
        this.toRemove = new ArrayList<>();
    }

    public String store(InputStream content, String filename) throws IOException {
        return store(content, filename, "");
    }

    public String store(byte[] content, String filename) {
        return store(content, filename, "");
    }

    public String store(InputStream content, String filename, String path) throws IOException {
        var bytes = content.readAllBytes();
        return store(bytes, filename, path);
    }

    public String store(byte[] content, String filename, String directory) {
        return store(new StorageObject(directory, filename, content));
    }

    public String store(StorageObject storageObject) {
        storageObjects.add(storageObject);
        return storageObject.getPath();
    }

    public void remove(String fullPath) {
        toRemove.add(fullPath);
    }

    public List<StorageObject> toStores() {
        return List.copyOf(storageObjects);
    }

    public List<String> toRemove() {
        return List.copyOf(toRemove);
    }

}