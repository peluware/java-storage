package com.peluware.storage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

/**
 * Referencia a un archivo destino de carga con metadatos opcionales para la firma de URL.
 * Extiende {@link StorageObjectRef} añadiendo {@code contentType} y {@code contentLength},
 * que los backends compatibles (S3, GCS) incluirán en la URL prefirmada para restringir
 * el tipo y tamaño del archivo que el cliente puede subir.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class StorageUploadRef extends StorageObjectRef {

    private final @Nullable String contentType;
    private final @Nullable Long contentLength;

    public StorageUploadRef(String directory, String fileName, @Nullable String contentType, @Nullable Long contentLength) {
        super(directory, fileName);
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public StorageUploadRef(String directory, String fileName) {
        this(directory, fileName, null, null);
    }

    public static StorageUploadRef from(StorageObjectRef ref) {
        if (ref instanceof StorageUploadRef u) return u;
        return new StorageUploadRef(ref.getDirectory(), ref.getFileName());
    }
}
