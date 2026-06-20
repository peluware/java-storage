package com.peluware.storage;

/**
 * Referencia a un archivo dentro de un almacen: su ruta y nombre.
 * Base común para operaciones de consulta ({@link StorageRequest}) y almacenamiento ({@link StorageObject}).
 */
public class StorageObjectRef {

    private final String directory;
    private final String fileName;
    private final String path;

    public StorageObjectRef(String directory, String fileName) {
        StorageAssertions.validFilename(fileName);
        StorageAssertions.validDirectory(directory);
        this.directory = StorageUtils.normalizeDirectory(directory);
        this.fileName = fileName;
        this.path = StorageUtils.buildPath(this.directory, this.fileName);
    }

    public StorageObjectRef(StorageObjectRef ref) {
        this.directory = ref.directory;
        this.fileName = ref.fileName;
        this.path = ref.path;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public String getDirectory() {
        return this.directory;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getPath() {
        return this.path;
    }

    public String toString() {
        return "StorageObjectRef(directory=" + this.getDirectory() + ", fileName=" + this.getFileName() + ")";
    }

    public static class Builder {
        private String directory;
        private String fileName;

        public Builder(StorageObjectRef ref) {
            this.directory = ref.directory;
            this.fileName = ref.fileName;
        }

        public Builder directory(String directory) {
            this.directory = directory;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public StorageObjectRef build() {
            return new StorageObjectRef(directory, fileName);
        }
    }
}