package com.peluware.storage.disk;

import com.peluware.storage.*;
import com.peluware.storage.exceptions.AlreadyFileExistsStorageException;
import com.peluware.storage.exceptions.FileNotFoundStorageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

import static com.peluware.storage.StorageUtils.constructFileInfo;
import static java.lang.System.getProperty;

@Slf4j
public class DiskStorage extends Storage {

    private final String storagePath;
    private static final String USER_DIR_PROPERTY = "{user.dir}";
    private static final String DEFAULT_STORAGE_PATH = USER_DIR_PROPERTY + "/uploads";

    public DiskStorage() {
        this(DEFAULT_STORAGE_PATH);
    }

    public DiskStorage(String storagePath) {
        this.storagePath = reolveStoragePath(storagePath);
        log.debug("Storage path {}", this.storagePath);
        createDirIfNotExists(this.storagePath);
    }

    private static String reolveStoragePath(String path) {
        if (path.contains(USER_DIR_PROPERTY)) {
            return path.replace(USER_DIR_PROPERTY, getProperty("user.dir"));
        }

        if (path.contains("{user.home}")) {
            return path.replace("{user.home}", getProperty("user.home"));
        }

        return path.endsWith("/") || path.endsWith("\\") ? path.substring(0, path.length() - 1) : path;
    }

    @Override
    protected void internalStore(final ToStore toStore) throws IOException {

        var path = toStore.getPath();

        createDirIfNotExists(storagePath + "/" + path);

        var fileName = toStore.getFileName();
        var file = new File(storagePath + "/" + path, fileName);
        var created = file.createNewFile();

        if (!created) throw new AlreadyFileExistsStorageException(toStore);


        try (var fos = new FileOutputStream(file)) {
            IOUtils.copy(toStore.getStream(), fos);
        }
    }

    @Override
    protected Optional<Stored> internalDownload(final PathFile pathFile) {
        return getFile(pathFile).map(file -> {
            try {
                var filename = pathFile.getFileName();
                var path = pathFile.getPath();

                return StorageUtils.constructStoredFile(
                        new FileInputStream(file),
                        file.length(),
                        filename,
                        path
                );
            } catch (FileNotFoundException e) {
                log.error("Error downloading file: {}", pathFile.getCompletePath(), e);
                return null;
            }
        });
    }


    @Override
    protected Optional<Stored.Info> internalInfo(final PathFile pathFile) {
        return getFile(pathFile).map(file -> {
            var filename = pathFile.getFileName();
            var path = pathFile.getPath();

            return constructFileInfo(filename, file.length(), path);
        });
    }

    @Override
    protected boolean internalExists(final PathFile pathFile) {
        return getFile(pathFile).isPresent();
    }

    @Override
    protected void internalRemove(final PathFile pathFile) {
        getFile(pathFile).ifPresentOrElse(
                f -> {
                    var deleted = f.delete();
                    if (!deleted) {
                        log.error("Error deleting file: {}", pathFile.getCompletePath());
                    }
                },
                () -> {
                    throw new FileNotFoundStorageException(pathFile);
                }
        );
    }

    @Override
    protected URL internalGenerateSignedUrl(PathFile pathFile, Duration duration) {
        throw new UnsupportedOperationException("Signed URLs are not supported in DiskStorage");
    }

    private Optional<File> getFile(PathFile pathFile) {
        var path = pathFile.getPath();
        var file = new File(storagePath + "/" + path, pathFile.getFileName());
        return file.exists() ? Optional.of(file) : Optional.empty();
    }

    private void createDirIfNotExists(String path) {
        var dirs = new File(path);
        if (!dirs.exists()) {
            var created = dirs.mkdirs();
            if (!created) throw new IllegalStateException("Path not created: " + path);
        }
    }
}
