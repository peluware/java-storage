package com.peluware.storage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Referencia a un archivo dentro de un almacen: su ruta y nombre.
 * Base común para operaciones de consulta ({@link StorageRequest}) y almacenamiento ({@link StorageObject}).
 */
@Getter
@EqualsAndHashCode
@ToString
public class StorageObjectRef {

    private final String directory;
    private final String fileName;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
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