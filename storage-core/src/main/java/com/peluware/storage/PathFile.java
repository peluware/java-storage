package com.peluware.storage;

import lombok.Data;

@Data
public class PathFile {

    private final String path;
    private final String fileName;

    public PathFile(String path, String fileName) {
        StorageAssertions.validFilename(fileName);
        StorageAssertions.validPath(path);
        this.path = StorageUtils.normalizePath(path);
        this.fileName = fileName;
    }

    public String getCompletePath() {
        return StorageUtils.factoryPathFile(path, fileName);
    }

}
