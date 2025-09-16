package com.peluware.storage.exceptions;

import com.peluware.storage.PathFile;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class FileNotFoundStorageException extends StorageException {

    private final String filename;
    private final String path;

    private FileNotFoundStorageException(String filename, String path) {
        super("File not found: " + filename + " in " + path);
        this.filename = filename;
        this.path = path;
    }

    public FileNotFoundStorageException(PathFile pathFile) {
        this(pathFile.getFileName(), pathFile.getPath());
    }

    public static Supplier<StorageException> fileNotFound(String filename, String path) {
        return () -> new FileNotFoundStorageException(new PathFile(path, filename));
    }

}