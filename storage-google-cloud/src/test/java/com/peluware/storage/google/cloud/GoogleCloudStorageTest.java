package com.peluware.storage.google.cloud;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class GoogleCloudStorageTest {

    private static final Logger log = LoggerFactory.getLogger(GoogleCloudStorageTest.class);
    private static GoogleCloudStorage storage;

    @BeforeAll
    static void setUp() {
        var storage = StorageOptions.newBuilder()
            .setProjectId("test-project")
            // Apunta al emulador local
            .setHost("http://localhost:4443")
            // Desactiva credenciales (el emulador no las valida)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
        storage.create(BucketInfo.of("sona_app_test"));
        var bucket = storage.get("sona_app_test");
        GoogleCloudStorageTest.storage = new GoogleCloudStorage(bucket);

    }

    @Test
    void store() {
        log.info("Storing file");
        try (var resource = getResource("test-file.webp")) {

            var fullpath = storage.store(resource, "test.txt");

            System.out.printf("Stored file with fullpath: %s%n", fullpath);
            assertNotNull(fullpath);

        } catch (IOException e) {
            log.error("Error storing file {}", e.getMessage());
            fail(e);
        }
    }

    @Test
    void storeWithPath() {

        var fileName = "test-file.webp";
        try (var resource = getResource(fileName)) {

            var fullpath = storage.store(resource, fileName, "test_dir/");

            System.out.printf("Stored file with fullpath: %s%n", fullpath);
            assertNotNull(fullpath);

        } catch (IOException e) {
            fail(e);
        }

    }

    @Test
    void download() {

        var fileName = "to-download.webp";
        try (var resource = getResource(fileName)) {

            var fullpath = storage.store(resource, fileName);
            var downloaded = storage.get(fullpath);

            assertTrue(downloaded.isPresent());

            var stored = downloaded.get();
            log.info("Downloaded file with info {}", stored);

            try (var fos = new FileOutputStream("downloaded-" + fileName)) {
                IOUtils.copy(stored.openContent(), fos);
            }

            assertNotNull(stored);
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void exists() {

        var fileName = "test-file.webp";
        try (var resource = getResource(fileName)) {

            var fullPath = storage.store(resource, "to-exists" + fileName);

            assertTrue(storage.exists(fullPath));

        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void remove() {

        var fileName = "test-file.webp";
        try (var resource = getResource(fileName)) {

            var fullPath = storage.store(resource, "to-remove" + fileName);
            storage.remove(fullPath);

            assertFalse(storage.exists("to-remove" + fileName));
        } catch (IOException e) {
            fail(e);
        }
    }

    private static InputStream getResource(String resource) {
        var stream = GoogleCloudStorageTest.class.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found");
        }
        return stream;
    }
}