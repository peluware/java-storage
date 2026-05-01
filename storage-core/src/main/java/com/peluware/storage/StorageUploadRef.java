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

    public StorageUploadRef(StorageObjectRef ref, @Nullable String contentType, @Nullable Long contentLength) {
        super(ref);
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public StorageUploadRef(StorageObjectRef ref) {
        this(ref, null, null);
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder extends StorageObjectRef.Builder {

        private @Nullable String contentType;
        private @Nullable Long contentLength;

        public Builder(StorageUploadRef ref) {
            super(ref);
            this.contentType = ref.contentType;
            this.contentLength = ref.contentLength;
        }

        public Builder directory(String directory) {
            return (Builder) super.directory(directory);

        }

        public Builder fileName(String fileName) {
            return (Builder) super.fileName(fileName);
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        @Override
        public StorageUploadRef build() {
            return new StorageUploadRef(super.build(), contentType, contentLength);
        }
    }
}