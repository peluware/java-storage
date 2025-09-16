package com.peluware.storage.google.cloud;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.peluware.storage.PathFile;
import com.peluware.storage.Storage;
import com.peluware.storage.Stored;
import com.peluware.storage.ToStore;
import com.peluware.storage.exceptions.FileNotFoundStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    protected void internalStore(final ToStore toStore) {
        var contentType = guessContentType(toStore.getFileName());
        var blob = bucket.create(toStore.getCompletePath(), toStore.getStream(), contentType);
        log.debug("Stored blob: {}", blob.getName());
    }

    @Override
    protected Optional<Stored> internalDownload(final PathFile pathFile) {

        var filename = pathFile.getFileName();
        var path = pathFile.getPath();

        var blob = getBlob(pathFile.getCompletePath());

        if (blob == null || !blob.exists()) {
            return Optional.empty();
        }
        var reader = blob.reader();
        var inputStream = Channels.newInputStream(reader);

        return Optional.ofNullable(constructStoredFile(
                inputStream,
                blob.getSize(),
                filename,
                path,
                blob.getContentType()
        ));
    }

    @Override
    protected Optional<Stored.Info> internalInfo(final PathFile pathFile) {


        var blob = getBlob(pathFile.getCompletePath());
        if (blob == null || !blob.exists()) {
            return Optional.empty();
        }

        var filename = pathFile.getFileName();
        var path = pathFile.getPath();

        return Optional.ofNullable(constructFileInfo(
                filename,
                blob.getSize(),
                path,
                blob.getContentType()
        ));
    }

    @Override
    protected boolean internalExists(final PathFile pathFile) {
        var blob = getBlob(pathFile.getCompletePath());
        return blob != null && blob.exists();
    }

    @Override
    protected void internalRemove(final PathFile pathFile) {
        var blob = getBlob(pathFile.getCompletePath());
        chekcIfBlobExistis(pathFile, blob);
        blob.delete();
    }

    @Override
    protected URL internalGenerateSignedUrl(PathFile pathFile, Duration duration) {
        var blob = getBlob(pathFile.getCompletePath());
        chekcIfBlobExistis(pathFile, blob);
        return blob.signUrl(duration.toMinutes(), TimeUnit.MINUTES);
    }

    private static void chekcIfBlobExistis(PathFile pathFile, Blob blob) {
        if (blob == null) {
            throw new FileNotFoundStorageException(pathFile);
        }
    }

    private Blob getBlob(String blobName) {
        return bucket.get(blobName);
    }
}
