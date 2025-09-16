package com.peluware.storage.springframework.data.jpa;

import com.peluware.storage.PathFile;
import com.peluware.storage.Stored;
import com.peluware.storage.Storage;
import com.peluware.storage.ToStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.*;

@Slf4j
@RequiredArgsConstructor
public final class JpaStorage extends Storage {

    private final EntityManager entityManager;

    @Override
    protected void internalStore(final ToStore toStore) throws IOException {
        var filename = toStore.getFileName();
        var stream = toStore.getStream();
        var content = stream.readAllBytes();

        var dbFile = FileStored.builder()
                .content(content)
                .contentType(guessContentType(filename))
                .contentLength((long) content.length)
                .originalFileName(filename)
                .path(toStore.getCompletePath())
                .uploadedAt(LocalDateTime.now())
                .build();

        entityManager.persist(dbFile);
        log.debug("Stored file with jpa: {}", dbFile.getId());
    }

    @Override
    protected Optional<Stored> internalDownload(final PathFile pathFile) {
        var query = entityManager.createQuery(
                "SELECT f FROM FileStored f WHERE f.originalFileName = :filename AND f.path = :path",
                FileStored.class
        );
        query.setParameter("filename", pathFile.getFileName());
        query.setParameter("path", pathFile.getPath());

        return query.getResultStream().findFirst().map(dbFile -> {
            var contentStream = dbFile.getContent() != null
                    ? new ByteArrayInputStream(dbFile.getContent())
                    : InputStream.nullInputStream();

            return constructStoredFile(
                    contentStream,
                    dbFile.getContentLength(),
                    dbFile.getOriginalFileName(),
                    dbFile.getPath(),
                    dbFile.getContentType()
            );
        });
    }

    @Override
    protected Optional<Stored.Info> internalInfo(final PathFile pathFile) {
        // Criteria API para proyección tipada
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(FileStoredProjection.class);
        var root = cq.from(FileStored.class);

        cq.select(cb.construct(
                FileStoredProjection.class,
                root.get("contentLength"),
                root.get("contentType"),
                root.get("originalFileName"),
                root.get("path")
        )).where(
                cb.and(
                        cb.equal(root.get("originalFileName"), pathFile.getFileName()),
                        cb.equal(root.get("path"), pathFile.getPath())
                )
        );

        var resultOptional = entityManager.createQuery(cq)
                .getResultStream()
                .findFirst();

        return resultOptional.map(info ->
                constructFileInfo(
                        info.getOriginalFileName(),
                        info.getContentLength(),
                        info.getPath(),
                        info.getContentType()
                )
        );
    }

    @Override
    protected boolean internalExists(final PathFile pathFile) {
        var query = entityManager.createQuery("SELECT COUNT(f) FROM FileStored f WHERE f.originalFileName = :filename AND f.path = :path", Long.class);
        query.setParameter("filename", pathFile.getFileName());
        query.setParameter("path", pathFile.getPath());

        var count = query.getSingleResult();
        return count != null && count > 0;
    }

    @Override
    protected void internalRemove(final PathFile pathFile) {
        var query = entityManager.createQuery("DELETE FROM FileStored f WHERE f.originalFileName = :filename AND f.path = :path");
        query.setParameter("filename", pathFile.getFileName());
        query.setParameter("path", pathFile.getPath());
        query.executeUpdate();
    }

    @Override
    protected URL internalGenerateSignedUrl(PathFile pathFile, java.time.Duration duration) {
        throw new UnsupportedOperationException("Signed URLs are not supported in JpaStorage");
    }
}
