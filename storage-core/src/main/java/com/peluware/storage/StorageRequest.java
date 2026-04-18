package com.peluware.storage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Referencia a un archivo para operaciones de consulta: descarga, info, existencia,
 * eliminación y URL firmadas. Opcionalmente, incluye un {@link ByteRange} para
 * descargas parciales.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class StorageRequest extends StorageObjectRef {

    /** Rango de bytes solicitado. {@code null} significa el archivo completo. */
    private final @Nullable ByteRange range;

    public StorageRequest(String directory, String fileName) {
        this(directory, fileName, null);
    }

    public StorageRequest(String directory, String fileName, @Nullable ByteRange range) {
        super(directory, fileName);
        this.range = range;
    }
}
