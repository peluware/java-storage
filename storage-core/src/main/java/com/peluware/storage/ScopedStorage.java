package com.peluware.storage;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Storage que aplica un directorio base (scope) a todas las operaciones.
 * El scope se resuelve dinámicamente en cada operación via {@link Supplier}.
 */
public class ScopedStorage extends Storage {

    private final Storage delegate;
    private final Supplier<String> basePath;

    public ScopedStorage(Storage delegate, Supplier<String> basePath) {
        this.delegate = delegate;
        this.basePath = basePath;
    }

    public ScopedStorage(Storage delegate, String basePath) {
        if (!basePath.isBlank()) {
            StorageAssertions.validDirectory(basePath);
        }
        this.delegate = delegate;
        this.basePath = () -> basePath;
    }

    private String resolveBase() {
        var base = basePath.get();
        if (base.isBlank()) return "";
        StorageAssertions.validDirectory(base);
        return base.endsWith("/") ? base : base + "/";
    }

    private String resolve(String directory) {
        var base = resolveBase();
        if (directory.isBlank()) return base;
        return base + directory;
    }

    // =========================
    // INTERNALS (delegación)
    // =========================

    @Override
    protected void internalStore(StorageObject storageObject) throws IOException {
        var resolved = new StorageObject(
            resolve(storageObject.getDirectory()),
            storageObject.getFileName(),
            storageObject.getContent()
        );
        delegate.internalStore(resolved);
    }

    @Override
    protected Optional<Stored> internalDownload(StorageRequest request) throws IOException {
        return delegate.internalDownload(
            new StorageRequest(
                resolve(request.getDirectory()),
                request.getFileName(),
                request.getRange()
            )
        );
    }

    @Override
    protected Optional<Stored.Info> internalInfo(StorageObjectRef ref) throws IOException {
        return delegate.internalInfo(
            new StorageObjectRef(
                resolve(ref.getDirectory()),
                ref.getFileName()
            )
        );
    }

    @Override
    protected boolean internalExists(StorageObjectRef ref) throws IOException {
        return delegate.internalExists(
            new StorageObjectRef(
                resolve(ref.getDirectory()),
                ref.getFileName()
            )
        );
    }

    @Override
    protected void internalRemove(StorageObjectRef ref) throws IOException {
        delegate.internalRemove(
            new StorageObjectRef(
                resolve(ref.getDirectory()),
                ref.getFileName()
            )
        );
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        return delegate.internalGenerateDownloadSignedUrl(
            new StorageRequest(
                resolve(request.getDirectory()),
                request.getFileName(),
                request.getRange()
            ),
            duration
        );
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
        return delegate.internalGenerateUploadSignedUrl(
            new StorageUploadRef(
                resolve(ref.getDirectory()),
                ref.getFileName(),
                ref.getContentType(),
                ref.getContentLength()
            ),
            duration
        );
    }
}
