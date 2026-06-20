package com.peluware.storage.disk;

import com.peluware.storage.*;
import com.peluware.storage.exceptions.AlreadyExistsStorageObjectException;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.newStoredObject;
import static java.lang.System.getProperty;

public class DiskStorage extends Storage {

    private static final Logger log = LoggerFactory.getLogger(DiskStorage.class);
    private final Path storagePath;
    private static final String USER_DIR_PROPERTY = "{user.dir}";
    private static final String DEFAULT_STORAGE_PATH = USER_DIR_PROPERTY + "/uploads";

    public DiskStorage() {
        this(DEFAULT_STORAGE_PATH);
    }

    public DiskStorage(String storagePath) {
        this.storagePath = Paths.get(resolveStoragePath(storagePath));
        log.debug("Storage path {}", this.storagePath);
        createDirIfNotExists(this.storagePath);
    }

    private static String resolveStoragePath(String path) {
        if (path.contains(USER_DIR_PROPERTY)) {
            return path.replace(USER_DIR_PROPERTY, getProperty("user.dir"));
        }
        if (path.contains("{user.home}")) {
            return path.replace("{user.home}", getProperty("user.home"));
        }
        return path;
    }

    @Override
    protected void internalStore(final StorageObject storageObject) throws IOException {
        var dir = storageObject.getDirectory();
        var fileName = storageObject.getFileName();

        var fullDir = storagePath.resolve(dir);
        createDirIfNotExists(fullDir);

        var filePath = fullDir.resolve(fileName);
        Files.createFile(filePath);

        try (var fos = Files.newOutputStream(filePath)) {
            IOUtils.copy(storageObject.getContent(), fos);
        }
    }

    @Override
    protected Optional<StoredObject> internalGet(final StorageRequest request) {
        var optionalPath = getFilePath(request);
        if (optionalPath.isEmpty()) return Optional.empty();

        var path = optionalPath.get();
        var fileSize = uncheckedSize(path);
        var range = request.getRange();

        final long contentLength;
        if (range != null) {
            var end = range.isOpenEnd() ? fileSize - 1 : Math.min(range.end(), fileSize - 1);
            contentLength = end - range.start() + 1;
        } else {
            contentLength = fileSize;
        }

        StorageContentLoader loader = () -> {
            var raw = Files.newInputStream(path);
            if (range != null) {
                raw.skipNBytes(range.start());
                return BoundedInputStream.builder()
                    .setInputStream(raw)
                    .setMaxCount(contentLength)
                    .get();
            }
            return raw;
        };

        return Optional.of(newStoredObject(
            loader,
            contentLength,
            request.getFileName(),
            request.getDirectory()));
    }

    private static long uncheckedSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected boolean internalExists(final StorageObjectRef ref) {
        return getFilePath(ref).isPresent();
    }

    @Override
    protected void internalRemove(final StorageObjectRef ref) {
        getFilePath(ref).ifPresentOrElse(
            path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.error("Error deleting file: {}", ref.getPath(), e);
                }
            },
            () -> {
                throw new StorageObjectNotFoundException(ref);
            }
        );
    }

    @Override
    protected List<StoredObject> internalList(String directory) throws IOException {
        var dir = storagePath.resolve(directory);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return List.of();
        var entries = new ArrayList<StoredObject>();
        try (var stream = Files.list(dir)) {
            Iterable<Path> it = stream::iterator;
            for (var file : it) {

                if (!Files.isRegularFile(file)) continue;
                var filename = file.getFileName().toString();

                entries.add(newStoredObject(
                    () -> Files.newInputStream(file),
                    Files.size(file),
                    filename,
                    directory
                ));
            }
        }
        return List.copyOf(entries);
    }

    @Override
    protected void internalMove(StorageObjectRef source, StorageObjectRef target) throws IOException {
        var srcPath = refToPath(source);
        if (!Files.exists(srcPath)) throw new StorageObjectNotFoundException(source);

        var tgtDir = storagePath.resolve(target.getDirectory());
        var tgtPath = tgtDir.resolve(target.getFileName());
        if (Files.exists(tgtPath)) throw new AlreadyExistsStorageObjectException(target);

        createDirIfNotExists(tgtDir);
        Files.move(srcPath, tgtPath);
        log.debug("Moved file: {} -> {}", source.getPath(), target.getPath());
    }

    @Override
    protected void internalCopy(StorageObjectRef source, StorageObjectRef target) throws IOException {
        var srcPath = refToPath(source);
        if (!Files.exists(srcPath)) throw new StorageObjectNotFoundException(source);
        var tgtDir = storagePath.resolve(target.getDirectory());
        createDirIfNotExists(tgtDir);
        Files.copy(srcPath, tgtDir.resolve(target.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        log.debug("Copied file: {} -> {}", source.getPath(), target.getPath());
    }

    @Override
    protected URL internalGenerateDownloadSignedUrl(StorageRequest request, Duration duration) {
        throw new UnsupportedOperationException("Signed URLs are not supported in DiskStorage");
    }

    @Override
    protected URL internalGenerateUploadSignedUrl(StorageUploadRef ref, Duration duration) {
        throw new UnsupportedOperationException("Upload signed URLs are not supported in DiskStorage");
    }

    @Override
    protected URL internalGenerateDeleteSignedUrl(StorageObjectRef ref, Duration duration) {
        throw new UnsupportedOperationException("Delete signed URLs are not supported in DiskStorage");
    }

    private Path refToPath(StorageObjectRef source) {
        return storagePath
            .resolve(source.getDirectory())
            .resolve(source.getFileName());
    }

    private Optional<Path> getFilePath(StorageObjectRef ref) {
        var filePath = refToPath(ref);

        return Files.exists(filePath)
            ? Optional.of(filePath)
            : Optional.empty();
    }

    private void createDirIfNotExists(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new IllegalStateException("Path not created: " + path, e);
            }
        }
    }
}
