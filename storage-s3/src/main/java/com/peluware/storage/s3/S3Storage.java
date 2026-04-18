package com.peluware.storage.s3;

import com.peluware.storage.StorageObjectRef;
import com.peluware.storage.StorageRequest;
import com.peluware.storage.Storage;
import com.peluware.storage.Stored;
import com.peluware.storage.StorageObject;
import com.peluware.storage.exceptions.StorageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.*;

@Slf4j
public class S3Storage extends Storage {

    private final S3Client client;
    private final @Nullable S3Presigner presigner;
    private final String bucketName;

    public S3Storage(S3Client client, @Nullable S3Presigner presigner, String bucketName) {
        this.client = client;
        this.presigner = presigner;
        this.bucketName = bucketName;
    }

    public S3Storage(S3Client client, String bucketName) {
        this(client, null, bucketName);
    }

    @Override
    protected void internalStore(StorageObject storageObject) throws IOException {
        var key = storageObject.getPath();
        var contentType = guessContentType(storageObject.getFileName());

        final RequestBody requestBody;
        if (storageObject.getContentLength() != null) {
            requestBody = RequestBody.fromInputStream(storageObject.getContent(), storageObject.getContentLength());
        } else {
            requestBody = RequestBody.fromBytes(storageObject.getContent().readAllBytes());
        }

        client.putObject(r -> r.bucket(bucketName).key(key).contentType(contentType), requestBody);
        log.debug("Stored S3 object: {}", key);
    }

    @Override
    protected Optional<Stored> internalDownload(StorageRequest request) {
        try {
            var builder = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(request.getPath());

            if (request.getRange() != null) {
                builder.range(request.getRange().toHttpHeader());
            }

            var response = client.getObject(builder.build());
            var meta = response.response();
            var contentType = meta.contentType() != null
                ? meta.contentType()
                : guessContentType(request.getFileName());

            return Optional.of(constructStoredFile(
                response,
                meta.contentLength(),
                request.getFileName(),
                request.getDirectory(),
                contentType
            ));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    protected Optional<Stored.Info> internalInfo(StorageObjectRef ref) {
        try {
            var response = client.headObject(r -> r.bucket(bucketName).key(ref.getPath()));
            var contentType = response.contentType() != null
                ? response.contentType()
                : guessContentType(ref.getFileName());

            return Optional.of(constructFileInfo(
                ref.getFileName(),
                response.contentLength(),
                ref.getDirectory(),
                contentType
            ));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    protected boolean internalExists(StorageObjectRef ref) {
        try {
            client.headObject(r -> r.bucket(bucketName).key(ref.getPath()));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    protected void internalRemove(StorageObjectRef ref) {
        if (!internalExists(ref)) throw new StorageNotFoundException(ref);
        client.deleteObject(r -> r.bucket(bucketName).key(ref.getPath()));
        log.debug("Deleted S3 object: {}", ref.getPath());
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        if (presigner == null) {
            throw new UnsupportedOperationException("S3Presigner not configured. Use the constructor that accepts an S3Presigner.");
        }
        var presigned = presigner.presignGetObject(r -> r
                .signatureDuration(duration)
                .getObjectRequest(req -> {
                    var builder = req.bucket(bucketName).key(request.getPath());
                    if (request.getRange() != null) {
                        builder.range(request.getRange().toHttpHeader());
                    }
                })
        );
        return presigned.url();
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageObjectRef ref, Duration duration) {
        if (presigner == null) {
            throw new UnsupportedOperationException("S3Presigner not configured. Use the constructor that accepts an S3Presigner.");
        }
        var presigned = presigner.presignPutObject(r -> r
                .signatureDuration(duration)
                .putObjectRequest(req -> req.bucket(bucketName).key(ref.getPath()))
        );
        return presigned.url();
    }
}
