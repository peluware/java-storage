package com.peluware.storage.google.cloud;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.HttpMethod;
import com.peluware.storage.StorageObject;
import com.peluware.storage.StorageObjectRef;
import com.peluware.storage.StorageRequest;
import com.peluware.storage.Storage;
import com.peluware.storage.Stored;
import com.peluware.storage.exceptions.StorageNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    protected Optional<Stored> internalDownload(final StorageRequest request) throws IOException {
        var blob = getBlob(request.getPath());
        if (blob == null || !blob.exists()) return Optional.empty();

        var filename = request.getFileName();
        var contentType = blob.getContentType() != null ? blob.getContentType() : guessContentType(filename);
        var range = request.getRange();

        var reader = blob.reader();
        final long contentLength;
        long blobSize = blob.getSize();

        if (range != null) {
            long start = range.start();

            if (start >= blobSize) {
                return Optional.empty();
            }

            long end = range.isOpenEnd()
                ? blobSize - 1
                : Math.min(range.end(), blobSize - 1);

            if (end < start) {
                return Optional.empty();
            }

            reader.seek(start);

            long limit = end - start + 1;
            reader.limit(limit);
            contentLength = limit;

        } else {
            contentLength = blobSize;
        }

        final InputStream stream = Channels.newInputStream(reader);

        return Optional.of(constructStoredFile(
            stream,
            contentLength,
            filename,
            request.getDirectory(),
            contentType
        ));
    }

    @Override
    protected Optional<Stored.Info> internalInfo(final StorageObjectRef ref) {
        var blob = getBlob(ref.getPath());
        if (blob == null || !blob.exists()) return Optional.empty();

        var contentType = blob.getContentType() != null ? blob.getContentType() : guessContentType(ref.getFileName());
        return Optional.of(constructFileInfo(ref.getFileName(), blob.getSize(), ref.getDirectory(), contentType));
    }

    @Override
    protected boolean internalExists(final StorageObjectRef ref) {
        var blob = getBlob(ref.getPath());
        return blob != null && blob.exists();
    }

    @Override
    protected void internalRemove(final StorageObjectRef ref) {
        var blob = getBlob(ref.getPath());
        if (blob == null) throw new StorageNotFoundException(ref);
        blob.delete();
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        var blob = getBlob(request.getPath());
        if (blob == null) throw new StorageNotFoundException(request);
        return blob.signUrl(duration.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageObjectRef ref, Duration duration) {
        var blobInfo = BlobInfo.newBuilder(bucket.getName(), ref.getPath()).build();
        com.google.cloud.storage.Storage gcsStorage = bucket.getStorage();
        return gcsStorage.signUrl(
            blobInfo,
            duration.toSeconds(), TimeUnit.SECONDS,
            com.google.cloud.storage.Storage.SignUrlOption.httpMethod(HttpMethod.PUT)
        );
    }

    private @Nullable Blob getBlob(String blobName) {
        return bucket.get(blobName);
    }
}
