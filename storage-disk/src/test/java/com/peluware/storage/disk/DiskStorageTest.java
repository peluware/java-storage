package com.peluware.storage.disk;

import com.peluware.storage.StorageObject;
import com.peluware.storage.exceptions.AlreadyFileExistsStorageException;
import com.peluware.storage.exceptions.StorageNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DiskStorageTest {

    @TempDir
    Path tempDir;

    private DiskStorage storage;

    @BeforeEach
    void setUp() {
        storage = new DiskStorage(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup is handled by @TempDir
    }

    @Test
    void testConstructorWithDefaultPath() {
        var defaultStorage = new DiskStorage();
        assertNotNull(defaultStorage);
    }

    @Test
    void testConstructorWithCustomPath() {
        var customStorage = new DiskStorage(tempDir.toString());
        assertNotNull(customStorage);
        assertTrue(Files.exists(tempDir));
    }

    @Test
    void testConstructorWithUserDirProperty() {
        var userDirStorage = new DiskStorage("{user.dir}/test-storage");
        assertNotNull(userDirStorage);
    }

    @Test
    void testConstructorWithUserHomeProperty() {
        var userHomeStorage = new DiskStorage("{user.home}/test-storage");
        assertNotNull(userHomeStorage);
    }

    @Test
    void testStoreFile() throws IOException {
        var content = "Test content".getBytes();
        var filename = "test.txt";
        var path = "documents";

        var fullPath = storage.store(content, filename, path);

        assertEquals("documents/test.txt", fullPath);
        assertTrue(storage.exists(filename, path));
    }

    @Test
    void testStoreFileInRootPath() throws IOException {
        var content = "Root content".getBytes();
        var filename = "root-file.txt";

        var fullPath = storage.store(content, filename);

        assertTrue(storage.exists(fullPath));
    }

    @Test
    void testStoreFileWithInputStream() throws IOException {
        var content = "Stream content".getBytes();
        var stream = new ByteArrayInputStream(content);
        var filename = "stream-file.txt";
        var path = "uploads";

        var fullPath = storage.store(stream, filename, path);

        assertTrue(storage.exists(fullPath));
    }

    @Test
    void testStoreFileAlreadyExists() throws IOException {
        var content = "Content".getBytes();
        var filename = "duplicate.txt";
        var path = "files";

        storage.store(content, filename, path);

        assertThrows(AlreadyFileExistsStorageException.class, () -> {
            storage.store(content, filename, path);
        });
    }

    @Test
    void testStoreMultipleFiles() throws IOException {
        var toStore1 = new StorageObject("path1", "file1.txt", "Content 1".getBytes());
        var toStore2 = new StorageObject("path2", "file2.txt", "Content 2".getBytes());

        storage.store(toStore1, toStore2);

        assertTrue(storage.exists("file1.txt", "path1"));
        assertTrue(storage.exists("file2.txt", "path2"));
    }

    @Test
    void testStoreMultipleFilesWithRollback() throws IOException {
        var toStore1 = new StorageObject("rollback", "file1.txt", "Content 1".getBytes());
        storage.store(toStore1);

        var toStore2 = new StorageObject("rollback", "file2.txt", "Content 2".getBytes());
        var toStore3 = new StorageObject("rollback", "file1.txt", "Duplicate".getBytes()); // Ya existe

        assertThrows(AlreadyFileExistsStorageException.class, () -> {
            storage.store(toStore2, toStore3);
        });

        // file2.txt no debería existir debido al rollback
        assertFalse(storage.exists("file2.txt", "rollback"));
    }

    @Test
    void testDownloadFile() throws IOException {
        var content = "Download content".getBytes();
        var filename = "download.txt";
        var path = "files";

        storage.store(content, filename, path);
        var stored = storage.download(filename, path);

        assertTrue(stored.isPresent());
        assertEquals(filename, stored.get().getInfo().getFileName());
        assertEquals(path, stored.get().getInfo().getDirectory());
        assertNotNull(stored.get().getStream());
    }

    @Test
    void testDownloadFileByFullPath() throws IOException {
        var content = "Full path content".getBytes();
        var fullPath = storage.store(content, "file.txt", "downloads");

        var stored = storage.download(fullPath);

        assertTrue(stored.isPresent());
        assertEquals("file.txt", stored.get().getInfo().getFileName());
    }

    @Test
    void testDownloadNonExistentFile() throws IOException {
        var stored = storage.download("nonexistent.txt", "nowhere");
        assertTrue(stored.isEmpty());
    }

    @Test
    void testFileInfo() throws IOException {
        var content = "Info content".getBytes();
        var filename = "info.txt";
        var path = "data";

        storage.store(content, filename, path);
        var info = storage.info(filename, path);

        assertTrue(info.isPresent());
        assertEquals(filename, info.get().getFileName());
        assertEquals(path, info.get().getDirectory());
        assertEquals(content.length, info.get().getFileSize());
    }

    @Test
    void testFileInfoByFullPath() throws IOException {
        var content = "Info by path".getBytes();
        var fullPath = storage.store(content, "info2.txt", "metadata");

        var info = storage.info(fullPath);

        assertTrue(info.isPresent());
        assertEquals(content.length, info.get().getFileSize());
    }

    @Test
    void testFileInfoNonExistent() throws IOException {
        var info = storage.info("ghost.txt", "nowhere");
        assertTrue(info.isEmpty());
    }

    @Test
    void testFileExists() throws IOException {
        var content = "Exists test".getBytes();
        var filename = "exists.txt";
        var path = "check";

        assertFalse(storage.exists(filename, path));

        storage.store(content, filename, path);

        assertTrue(storage.exists(filename, path));
    }

    @Test
    void testFileExistsByFullPath() throws IOException {
        var content = "Full path exists".getBytes();
        var fullPath = storage.store(content, "exists2.txt", "validation");

        assertTrue(storage.exists(fullPath));
    }

    @Test
    void testRemoveFile() throws IOException {
        var content = "Remove me".getBytes();
        var filename = "remove.txt";
        var path = "trash";

        storage.store(content, filename, path);
        assertTrue(storage.exists(filename, path));

        storage.remove(filename, path);
        assertFalse(storage.exists(filename, path));
    }

    @Test
    void testRemoveFileByFullPath() throws IOException {
        var content = "Remove by path".getBytes();
        var fullPath = storage.store(content, "remove2.txt", "bin");

        assertTrue(storage.exists(fullPath));

        storage.remove(fullPath);
        assertFalse(storage.exists(fullPath));
    }

    @Test
    void testRemoveNonExistentFile() {
        assertThrows(StorageNotFoundException.class, () -> {
            storage.remove("ghost.txt", "nowhere");
        });
    }

    @Test
    void testTransferTo() throws IOException {
        var targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        var targetStorage = new DiskStorage(targetDir.toString());

        var content = "Transfer content".getBytes();
        var filename = "transfer.txt";
        var path = "source";

        storage.store(content, filename, path);
        storage.transferTo(targetStorage, filename, path);

        assertTrue(targetStorage.exists(filename, path));
    }

    @Test
    void testTransferToByFullPath() throws IOException {
        var targetDir = tempDir.resolve("target2");
        Files.createDirectories(targetDir);
        var targetStorage = new DiskStorage(targetDir.toString());

        var content = "Transfer by path".getBytes();
        var fullPath = storage.store(content, "transfer2.txt", "origin");

        storage.transferTo(targetStorage, fullPath);

        assertTrue(targetStorage.exists(fullPath));
    }

    @Test
    void testNestedDirectoryCreation() throws IOException {
        var content = "Nested content".getBytes();
        var filename = "nested.txt";
        var path = "level1/level2/level3";

        var fullPath = storage.store(content, filename, path);

        assertTrue(storage.exists(fullPath));
        var diskPath = tempDir.resolve(path).resolve(filename);
        assertTrue(Files.exists(diskPath));
    }

    @Test
    void testStoreWithEmptyPath() throws IOException {
        var content = "Empty path content".getBytes();
        var filename = "empty-path.txt";

        var fullPath = storage.store(content, filename, "");

        assertTrue(storage.exists(fullPath));
    }

    @Test
    void testConcurrentFileOperations() throws IOException {
        var content1 = "Concurrent 1".getBytes();
        var content2 = "Concurrent 2".getBytes();

        var fullPath1 = storage.store(content1, "concurrent1.txt", "parallel");
        var fullPath2 = storage.store(content2, "concurrent2.txt", "parallel");

        assertTrue(storage.exists(fullPath1));
        assertTrue(storage.exists(fullPath2));
    }
}