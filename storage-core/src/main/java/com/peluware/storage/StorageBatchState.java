package com.peluware.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class StorageBatchState {
    private final List<ToStore> toStores;
    private final List<String> toRemove;

    public StorageBatchState() {
        this.toStores = new ArrayList<>();
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

    public String store(byte[] content, String filename, String path) {
        return store(new ToStore(path, filename, content));
    }

    public String store(ToStore toStore) {
        toStores.add(toStore);
        return toStore.getCompletePath();
    }

    public void remove(String fullPath) {
        toRemove.add(fullPath);
    }

    public List<ToStore> toStores() {
        return List.copyOf(toStores);
    }

    public List<String> toRemove() {
        return List.copyOf(toRemove);
    }

}