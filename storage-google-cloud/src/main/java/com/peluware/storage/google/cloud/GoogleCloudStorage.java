package com.peluware.storage.google.cloud;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.Storage.SignUrlOption;
import com.peluware.storage.*;
import com.peluware.storage.exceptions.AlreadyExistsStorageObjectException;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.nio.channels.Channels;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static com.peluware.storage.StorageUtils.*;

@Slf4j
@RequiredArgsConstructor
public class GoogleCloudStorage extends Storage {

    private final Bucket bucket;

    @Override
    protected void internalStore(final StorageObject storageObject) {
        var contentType = guessContentType(storageObject.getFileName());
        var blob = bucket.create(storageObject.getPath(), storageObject.getContent(), contentType);
        log.debug("Stored blob: {}", blob.getName());
    }

    @Override
    protected Optional<StoredObject> internalGet(final StorageRequest request) {
        var blob = getBlob(request.getPath());
        if (blob == null || !blob.exists()) return Optional.empty();

        var filename = request.getFileName();
        var contentType = blob.getContentType() != null ? blob.getContentType() : guessContentType(filename);
        var range = request.getRange();
        var blobSize = blob.getSize();

        final long contentLength;
        if (range != null) {
            var start = range.start();
            if (start >= blobSize) return Optional.empty();
            var end = range.isOpenEnd() ? blobSize - 1 : Math.min(range.end(), blobSize - 1);
            if (end < start) return Optional.empty();
            contentLength = end - start + 1;
        } else {
            contentLength = blobSize;
        }

        return Optional.of(constructStoredFile(
            () -> {
                var reader = blob.reader();
                if (range != null) {
                    reader.seek(range.start());
                    reader.limit(contentLength);
                }
                return Channels.newInputStream(reader);
            },
            contentLength,
            filename,
            request.getDirectory(),
            contentType
        ));
    }

    @Override
    protected boolean internalExists(final StorageObjectRef ref) {
        var blob = getBlob(ref.getPath());
        return blob != null && blob.exists();
    }

    @Override
    protected void internalRemove(final StorageObjectRef ref) {
        var blob = getBlob(ref.getPath());
        if (blob == null) throw new StorageObjectNotFoundException(ref);
        blob.delete();
    }

    @Override
    protected List<StoredObject> internalList(String directory) {
        var prefix = directory.isBlank() ? "" : (directory.endsWith("/") ? directory : directory + "/");
        var blobs = bucket.list(BlobListOption.prefix(prefix), BlobListOption.currentDirectory());
        return StreamSupport.stream(blobs.iterateAll().spliterator(), false)
            .filter(blob -> !blob.isDirectory())
            .map(blob -> {
                var filename = blob.getName().substring(prefix.length());
                var contentType = blob.getContentType() != null
                    ? blob.getContentType()
                    : guessContentType(filename);

                return constructStoredFile(
                    () -> Channels.newInputStream(blob.reader()),
                    blob.getSize(),
                    filename,
                    directory,
                    contentType
                );
            })
            .toList();
    }

    @Override
    protected void internalMove(StorageObjectRef source, StorageObjectRef target) {
        var blob = getBlob(source.getPath());
        if (blob == null || !blob.exists()) throw new StorageObjectNotFoundException(source);
        if (internalExists(target)) throw new AlreadyExistsStorageObjectException(target);
        blob.copyTo(bucket.getName(), target.getPath());
        blob.delete();
        log.debug("Moved GCS blob: {} -> {}", source.getPath(), target.getPath());
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        var blob = getBlob(request.getPath());
        if (blob == null) throw new StorageObjectNotFoundException(request);
        return blob.signUrl(duration.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
        var blobBuilder = BlobInfo.newBuilder(bucket.getName(), ref.getPath());
        if (ref.getContentType() != null) blobBuilder.setContentType(ref.getContentType());
        var gcsStorage = bucket.getStorage();
        return gcsStorage.signUrl(
            blobBuilder.build(),
            duration.toSeconds(), TimeUnit.SECONDS,
            SignUrlOption.httpMethod(HttpMethod.PUT)
        );
    }

    @Override
    public void close() throws Exception {
        bucket.getStorage().close();
    }

    private @Nullable Blob getBlob(String blobName) {
        return bucket.get(blobName);
    }
}
