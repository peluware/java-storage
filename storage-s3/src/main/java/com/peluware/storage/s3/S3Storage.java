package com.peluware.storage.s3;

import com.peluware.storage.*;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.constructStoredFile;
import static com.peluware.storage.StorageUtils.guessContentType;

public class S3Storage extends Storage {

    private static final Logger log = LoggerFactory.getLogger(S3Storage.class);
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
    protected void internalStore(StorageObject storageObject) {
        var key = storageObject.getPath();
        var contentType = guessContentType(storageObject.getFileName());

        var contentLength = storageObject.getContentLength();
        if (contentLength == null) {
            throw new IllegalArgumentException("S3 storage requires contentLength for object: " + key);
        }

        var requestBody = RequestBody.fromInputStream(
            storageObject.getContent(),
            contentLength
        );

        client.putObject(r -> r
                .bucket(bucketName)
                .key(key)
                .contentType(contentType),
            requestBody
        );

        log.debug("Stored S3 object: {} ({} bytes)", key, contentLength);
    }

    @Override
    protected Optional<StoredObject> internalGet(StorageRequest request) {
        try {
            var builder = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(request.getPath());

            var range = request.getRange();
            if (range != null) builder.range(range.toHttpHeader());

            var response = client.getObject(builder.build());
            var meta = response.response();

            var contentType = meta.contentType() != null
                ? meta.contentType()
                : guessContentType(request.getFileName());

            return Optional.of(constructStoredFile(
                () -> response,
                meta.contentLength(),
                request.getFileName(),
                request.getDirectory(),
                contentType
            ));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return Optional.empty();
            throw e;
        }
    }

    @Override
    protected boolean internalExists(StorageObjectRef ref) {
        try {
            client.headObject(r -> r.bucket(bucketName).key(ref.getPath()));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    @Override
    protected void internalRemove(StorageObjectRef ref) {
        client.deleteObject(r -> r.bucket(bucketName).key(ref.getPath()));
        log.debug("Deleted S3 object: {}", ref.getPath());
    }

    @Override
    protected List<StoredObject> internalList(String directory) {
        var prefix = directory.isBlank() ? "" : (directory.endsWith("/") ? directory : directory + "/");
        var request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .delimiter("/")
            .build();
        return client.listObjectsV2(request).contents().stream()
            .filter(obj -> !obj.key().equals(prefix))
            .map(obj -> {
                var key = obj.key();
                var filename = key.substring(prefix.length());
                return constructStoredFile(
                    () -> client.getObject(r -> r.bucket(bucketName).key(key)),
                    obj.size(),
                    filename,
                    directory
                );
            })
            .toList();
    }

    @Override
    protected void internalMove(StorageObjectRef source, StorageObjectRef target) {
        internalCopy(source, target);
        try {
            internalRemove(source);
        } catch (S3Exception e) {
            log.warn("Move copy succeeded but could not delete source: {}", source.getPath(), e);
        }
        log.debug("Moved S3 object: {} -> {}", source.getPath(), target.getPath());
    }


    @Override
    protected void internalCopy(StorageObjectRef source, StorageObjectRef target) {
        var sourcePath = source.getPath();
        var targetPath = target.getPath();
        try {
            client.copyObject(r -> r
                .sourceBucket(bucketName)
                .sourceKey(sourcePath)
                .destinationBucket(bucketName)
                .destinationKey(targetPath)
            );
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || "NoSuchKey".equals(e.awsErrorDetails().errorCode())) {
                throw new StorageObjectNotFoundException(source);
            }
            throw e;
        }
        log.debug("Copied S3 object: {} -> {}", sourcePath, targetPath);
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
    protected URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
        if (presigner == null) {
            throw new UnsupportedOperationException("S3Presigner not configured. Use the constructor that accepts an S3Presigner.");
        }
        var presigned = presigner.presignPutObject(r -> r
            .signatureDuration(duration)
            .putObjectRequest(req -> {
                var builder = req.bucket(bucketName).key(ref.getPath());
                if (ref.getContentType() != null) builder.contentType(ref.getContentType());
                if (ref.getContentLength() != null) builder.contentLength(ref.getContentLength());
            })
        );
        return presigned.url();
    }


    @Override
    protected URL internalGenerateDeleteSignedUrl(StorageObjectRef ref, Duration duration) {
        if (presigner == null) {
            throw new UnsupportedOperationException("S3Presigner not configured. Use the constructor that accepts an S3Presigner.");
        }
        var presigned = presigner.presignDeleteObject(r -> r
            .signatureDuration(duration)
            .deleteObjectRequest(req -> req.bucket(bucketName).key(ref.getPath()))
        );
        return presigned.url();
    }

    @Override
    public void close() {
        client.close();
        if (presigner != null) presigner.close();
    }
}
