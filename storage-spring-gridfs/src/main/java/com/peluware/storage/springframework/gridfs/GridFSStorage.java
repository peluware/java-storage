package com.peluware.storage.springframework.gridfs;

import com.mongodb.BasicDBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.peluware.storage.StorageObjectRef;
import com.peluware.storage.StorageUploadRef;
import com.peluware.storage.StorageRequest;
import com.peluware.storage.StoredObject;
import com.peluware.storage.Storage;
import com.peluware.storage.StorageObject;
import com.peluware.storage.exceptions.AlreadyExistsStorageObjectException;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import com.peluware.storage.exceptions.StorageException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.*;


/**
 * Servicio para operaciones relacionadas con archivos.
 */
@RequiredArgsConstructor
public final class GridFSStorage extends Storage {

    private final GridFsTemplate template;
    private final GridFsOperations operations;
    private final MongoTemplate mongoTemplate;
    private static final String DIR_KEY = "directory";
    private static final String FILES_COLLECTION = "fs.files";

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
    protected Optional<StoredObject> internalGet(final StorageRequest request) {
        var gridFSFile = getOne(request);
        if (gridFSFile == null) return Optional.empty();

        var metadata = gridFSFile.getMetadata();
        if (metadata == null) throw new StorageException("Metadata not found for file: " + request.getPath());

        var path = metadata.getString(DIR_KEY);
        var contentType = metadata.containsKey("_contentType")
                ? metadata.getString("_contentType")
                : guessContentType(request.getFileName());

        var range = request.getRange();
        final long contentLength = range != null
            ? (range.isOpenEnd() ? gridFSFile.getLength() - range.start() : range.end() - range.start() + 1)
            : gridFSFile.getLength();

        return Optional.of(constructStoredFile(
            () -> {
                if (range != null) {
                    var raw = operations.getResource(gridFSFile).getInputStream();
                    raw.skipNBytes(range.start());
                    return new ByteArrayInputStream(raw.readNBytes((int) contentLength));
                }
                return operations.getResource(gridFSFile).getInputStream();
            },
            contentLength,
            request.getFileName(),
            path,
            contentType
        ));
    }

    @Override
    protected boolean internalExists(final StorageObjectRef ref) {
        return getOne(ref) != null;
    }

    @Override
    protected void internalRemove(final StorageObjectRef ref) {
        if (getOne(ref) == null) throw new StorageObjectNotFoundException(ref);
        template.delete(createQuery(ref));
    }

    @Override
    protected List<StoredObject> internalList(String directory) {
        var query = new Query(Criteria.where("metadata." + DIR_KEY).is(directory));
        var entries = new ArrayList<StoredObject>();
        template.find(query).forEach(file -> {
            var metadata = file.getMetadata();
            var contentType = metadata != null && metadata.containsKey("_contentType")
                ? metadata.getString("_contentType")
                : guessContentType(file.getFilename());
            entries.add(constructStoredFile(
                () -> operations.getResource(file).getInputStream(),
                file.getLength(),
                file.getFilename(),
                directory,
                contentType
            ));
        });
        return List.copyOf(entries);
    }

    @Override
    protected void internalMove(StorageObjectRef source, StorageObjectRef target) {
        if (!internalExists(source)) throw new StorageObjectNotFoundException(source);
        if (internalExists(target)) throw new AlreadyExistsStorageObjectException(target);
        mongoTemplate.updateFirst(
            createQuery(source),
            new Update()
                .set("filename", target.getFileName())
                .set("metadata." + DIR_KEY, target.getDirectory()),
            FILES_COLLECTION
        );
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        throw new UnsupportedOperationException("Generate signed URL is not supported in GridFSStorage.");
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
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
