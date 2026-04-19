package com.peluware.storage;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class DelegatingStorage extends Storage {

    protected abstract Storage getDelegate();

    @Override
    protected void internalStore(StorageObject storageObject) throws IOException {
        getDelegate().internalStore(storageObject);
    }

    @Override
    protected Optional<Stored> internalGet(StorageRequest request) {
        return getDelegate().internalGet(request);
    }

    @Override
    protected boolean internalExists(StorageObjectRef ref) {
        return getDelegate().internalExists(ref);
    }

    @Override
    protected void internalRemove(StorageObjectRef ref) throws IOException {
        getDelegate().internalRemove(ref);
    }

    @Override
    protected List<Stored> internalList(String directory) throws IOException {
        return getDelegate().internalList(directory);
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        return getDelegate().internalGenerateDownloadSignedUrl(request, duration);
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
        return getDelegate().internalGenerateUploadSignedUrl(ref, duration);
    }

    @Override
    public void close() throws Exception {
        getDelegate().close();
    }

    public static DelegatingStorage of(Supplier<Storage> supplier) {
        return new DelegatingStorage() {
            @Override
            protected Storage getDelegate() {
                return supplier.get();
            }
        };
    }

    public static DelegatingStorage of(Storage storage) {
        return of(() -> storage);
    }
}