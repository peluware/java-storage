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

    protected StorageObjectRef(String directory, String fileName) {
        StorageAssertions.validFilename(fileName);
        StorageAssertions.validDirectory(directory);
        this.directory = StorageUtils.normalizeDirectory(directory);
        this.fileName = fileName;
    }

    public String getPath() {
        return StorageUtils.buildPath(directory, fileName);
    }
}
