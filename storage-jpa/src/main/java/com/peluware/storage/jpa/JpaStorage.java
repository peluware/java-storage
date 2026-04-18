package com.peluware.storage.jpa;

import com.peluware.storage.StorageObjectRef;
import com.peluware.storage.StorageUploadRef;
import com.peluware.storage.StorageRequest;
import com.peluware.storage.Stored;
import com.peluware.storage.Storage;
import com.peluware.storage.StorageObject;
import com.peluware.storage.exceptions.StorageNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.*;

@Slf4j
@RequiredArgsConstructor
public final class JpaStorage extends Storage {

    private final EntityManager entityManager;

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
    protected Optional<Stored> internalDownload(final StorageRequest request) {
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

            return constructStoredFile(
                new ByteArrayInputStream(content),
                content.length,
                dbFile.getOriginalFileName(),
                dbFile.getDirectory(),
                dbFile.getContentType()
            );
        });
    }

    @Override
    protected Optional<Stored.Info> internalInfo(final StorageObjectRef ref) {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(FileStoredProjection.class);
        var root = cq.from(FileStored.class);

        cq.select(cb.construct(
            FileStoredProjection.class,
            root.get("contentLength"),
            root.get("contentType"),
            root.get("originalFileName"),
            root.get("directory")
        )).where(
            cb.and(
                cb.equal(root.get("originalFileName"), ref.getFileName()),
                cb.equal(root.get("directory"), ref.getDirectory())
            )
        );

        return entityManager.createQuery(cq)
            .getResultStream()
            .findFirst()
            .map(info -> constructFileInfo(
                info.getOriginalFileName(),
                info.getContentLength(),
                info.getPath(),
                info.getContentType()
            ));
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
        if (deleted == 0) throw new StorageNotFoundException(ref);
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        throw new UnsupportedOperationException("Signed URLs are not supported in JpaStorage");
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
        throw new UnsupportedOperationException("Upload signed URLs are not supported in JpaStorage");
    }
}
