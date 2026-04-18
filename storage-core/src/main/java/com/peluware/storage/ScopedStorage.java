package com.peluware.storage;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
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

    private Stored unscope(Stored stored) {
        var base = resolveBase();
        if (base.isBlank()) return stored;
        var dir = stored.getDirectory();
        var relative = dir.startsWith(base) ? dir.substring(base.length()) : dir;
        return stored.withDirectory(relative);
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
    protected Optional<Stored> internalGet(StorageRequest request) {
        return delegate.internalGet(
            new StorageRequest(
                resolve(request.getDirectory()),
                request.getFileName(),
                request.getRange()
            )
        ).map(this::unscope);
    }

    @Override
    protected boolean internalExists(StorageObjectRef ref) {
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
    protected List<Stored> internalList(String directory) throws IOException {
        return delegate.internalList(resolve(directory)).stream()
            .map(this::unscope)
            .toList();
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
