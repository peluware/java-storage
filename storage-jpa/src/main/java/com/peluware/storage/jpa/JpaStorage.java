package com.peluware.storage.jpa;

import com.peluware.storage.*;
import com.peluware.storage.exceptions.AlreadyExistsStorageObjectException;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.newStoredObject;
import static com.peluware.storage.StorageUtils.guessContentType;

public final class JpaStorage extends Storage {

    private static final Logger log = LoggerFactory.getLogger(JpaStorage.class);
    private final EntityManager entityManager;

    public JpaStorage(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    protected void internalStore(final StorageObject storageObject) throws IOException {
        var filename = storageObject.getFileName();
        var content = storageObject.getContent().readAllBytes();

        var dbFile = FileStored.builder()
            .content(content)
            .contentType(guessContentType(filename))
            .contentLength((long) content.length)
            .originalFileName(filename)
            .directory(storageObject.getDirectory())
            .uploadedAt(LocalDateTime.now())
            .build();

        entityManager.persist(dbFile);
        log.debug("Stored file with jpa: {}", dbFile.getId());
    }

    @Override
    protected Optional<StoredObject> internalGet(final StorageRequest request) {
        var query = entityManager.createQuery(
            "SELECT f FROM FileStored f WHERE f.originalFileName = :filename AND f.directory = :directory",
            FileStored.class
        );
        query.setParameter("filename", request.getFileName());
        query.setParameter("directory", request.getDirectory());

        return query.getResultStream().findFirst().map(dbFile -> {
            var rawContent = dbFile.getContent() != null ? dbFile.getContent() : new byte[0];
            var range = request.getRange();

            final byte[] content;
            if (range != null) {
                var start = (int) range.start();
                var end = range.isOpenEnd() ? rawContent.length - 1 : (int) Math.min(range.end(), rawContent.length - 1);
                content = Arrays.copyOfRange(rawContent, start, end + 1);
            } else {
                content = rawContent;
            }

            return StorageUtils.newStoredObject(
                () -> new ByteArrayInputStream(content),
                content.length,
                dbFile.getOriginalFileName(),
                dbFile.getDirectory(),
                dbFile.getContentType()
            );
        });
    }

    @Override
    protected boolean internalExists(final StorageObjectRef ref) {
        var query = entityManager.createQuery(
            "SELECT COUNT(f) FROM FileStored f WHERE f.originalFileName = :filename AND f.directory = :directory",
            Long.class
        );
        query.setParameter("filename", ref.getFileName());
        query.setParameter("directory", ref.getDirectory());
        return query.getSingleResult() > 0;
    }

    @Override
    protected void internalRemove(final StorageObjectRef ref) {
        var query = entityManager.createQuery(
            "DELETE FROM FileStored f WHERE f.originalFileName = :filename AND f.directory = :directory"
        );
        query.setParameter("filename", ref.getFileName());
        query.setParameter("directory", ref.getDirectory());
        int deleted = query.executeUpdate();
        if (deleted == 0) throw new StorageObjectNotFoundException(ref);
    }

    @Override
    protected List<StoredObject> internalList(String directory) {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(FileStoredProjection.class);
        var root = cq.from(FileStored.class);

        cq.select(cb.construct(
            FileStoredProjection.class,
            root.get("contentLength"),
            root.get("contentType"),
            root.get("originalFileName"),
            root.get("directory")
        )).where(cb.equal(root.get("directory"), directory));

        return entityManager.createQuery(cq).getResultList().stream()
            .map(info -> StorageUtils.newStoredObject(
                () -> {
                    var raw = entityManager.createQuery("SELECT f.content FROM FileStored f WHERE f.originalFileName = :filename AND f.directory = :directory", byte[].class)
                        .setParameter("filename", info.getOriginalFileName())
                        .setParameter("directory", directory)
                        .getResultStream().findFirst().orElse(new byte[0]);
                    return new ByteArrayInputStream(raw);
                },
                info.getContentLength(),
                info.getOriginalFileName(),
                directory,
                info.getContentType()
            ))
            .toList();
    }

    @Override
    protected void internalMove(StorageObjectRef source, StorageObjectRef target) {
        if (!internalExists(source)) throw new StorageObjectNotFoundException(source);
        if (internalExists(target)) throw new AlreadyExistsStorageObjectException(target);
        entityManager.createQuery(
                "UPDATE FileStored f " +
                    "SET " +
                    "f.originalFileName = :targetFilename, " +
                    "f.directory = :targetDirectory " +
                    "WHERE f.originalFileName = :sourceFilename AND f.directory = :sourceDirectory"
            )
            .setParameter("targetFilename", target.getFileName())
            .setParameter("targetDirectory", target.getDirectory())
            .setParameter("sourceFilename", source.getFileName())
            .setParameter("sourceDirectory", source.getDirectory())
            .executeUpdate();
        log.debug("Moved JPA file: {} -> {}", source.getPath(), target.getPath());
    }

    @Override
    protected void internalCopy(StorageObjectRef source, StorageObjectRef target) throws IOException {
        var stored = internalGet(new StorageRequest(source.getDirectory(), source.getFileName()));
        if (stored.isEmpty()) throw new StorageObjectNotFoundException(source);
        internalStore(new StorageObject(target.getDirectory(), target.getFileName(), stored.get().openContent()));
        log.debug("Copied JPA file: {} -> {}", source.getPath(), target.getPath());
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        throw new UnsupportedOperationException("Signed URLs are not supported in JpaStorage");
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
        throw new UnsupportedOperationException("Upload signed URLs are not supported in JpaStorage");
    }

    @Override
    protected URL internalGenerateDeleteSignedUrl(StorageObjectRef ref, Duration duration) {
        throw new UnsupportedOperationException("Delete signed URLs are not supported in JpaStorage");
    }
}
