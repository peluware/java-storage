package com.peluware.storage.springframework.gridfs;

import com.mongodb.BasicDBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.peluware.storage.StorageObjectRef;
import com.peluware.storage.StorageRequest;
import com.peluware.storage.Stored;
import com.peluware.storage.Storage;
import com.peluware.storage.StorageObject;
import com.peluware.storage.exceptions.StorageNotFoundException;
import com.peluware.storage.exceptions.StorageException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.*;


/**
 * Servicio para operaciones relacionadas con archivos.
 */
@RequiredArgsConstructor
public final class GridFSStorage extends Storage {

    private final GridFsTemplate template;
    private final GridFsOperations operations;
    private static final String DIR_KEY = "directory";

    @Override
    protected void internalStore(StorageObject storageObject) {
        var filename = storageObject.getFileName();
        var stream = storageObject.getContent();

        var metadata = new BasicDBObject();
        metadata.put(DIR_KEY, storageObject.getDirectory());
        metadata.put("dateUpload", LocalDateTime.now());
        template.store(stream, filename, guessContentType(filename), metadata);
    }

    @Override
    protected Optional<Stored> internalDownload(final StorageRequest request) throws IOException {
        var gridFSFile = getOne(request);
        if (gridFSFile == null) return Optional.empty();

        var metadata = gridFSFile.getMetadata();
        if (metadata == null) throw new StorageException("Metadata not found for file: " + request.getPath());

        var path = metadata.getString(DIR_KEY);
        var contentType = metadata.containsKey("_contentType")
                ? metadata.getString("_contentType")
                : guessContentType(request.getFileName());

        var range = request.getRange();
        final InputStream stream;
        final long contentLength;

        if (range != null) {
            // GridFS doesn't support native range seeking: buffer the range bytes in memory
            var raw = operations.getResource(gridFSFile).getInputStream();
            raw.skipNBytes(range.start());
            contentLength = range.isOpenEnd() ? gridFSFile.getLength() - range.start() : range.end() - range.start() + 1;
            stream = new ByteArrayInputStream(raw.readNBytes((int) contentLength));
        } else {
            stream = operations.getResource(gridFSFile).getInputStream();
            contentLength = gridFSFile.getLength();
        }

        return Optional.of(constructStoredFile(
            stream,
            contentLength,
            request.getFileName(),
            path,
            contentType
        ));
    }

    @Override
    protected Optional<Stored.Info> internalInfo(final StorageObjectRef ref) {
        var gridFSFile = getOne(ref);
        if (gridFSFile == null) return Optional.empty();

        var metadata = gridFSFile.getMetadata();
        if (metadata == null) throw new StorageException("Metadata not found for file: " + ref.getPath());

        var path = metadata.getString(DIR_KEY);
        var contentType = metadata.containsKey("_contentType")
                ? metadata.getString("_contentType")
                : guessContentType(ref.getFileName());
        return Optional.of(constructFileInfo(ref.getFileName(), gridFSFile.getLength(), path, contentType));
    }

    @Override
    protected boolean internalExists(final StorageObjectRef ref) {
        return getOne(ref) != null;
    }

    @Override
    protected void internalRemove(final StorageObjectRef ref) {
        if (getOne(ref) == null) throw new StorageNotFoundException(ref);
        template.delete(createQuery(ref));
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        throw new UnsupportedOperationException("Generate signed URL is not supported in GridFSStorage.");
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageObjectRef ref, Duration duration) {
        throw new UnsupportedOperationException("Upload signed URLs are not supported in GridFSStorage.");
    }

    private static Query createQuery(final StorageObjectRef ref) {
        return new Query(
            Criteria
                .where("filename").is(ref.getFileName())
                .and("metadata." + DIR_KEY).is(ref.getDirectory())
        );
    }

    @SuppressWarnings("all")
    private @Nullable GridFSFile getOne(StorageObjectRef ref) {
        return template.findOne(createQuery(ref));
    }
}
