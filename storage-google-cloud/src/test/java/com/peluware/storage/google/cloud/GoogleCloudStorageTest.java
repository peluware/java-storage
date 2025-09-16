package com.peluware.storage.google.cloud;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class GoogleCloudStorageTest {

    private static GoogleCloudStorage storage;

    @BeforeAll
    static void setUp() throws IOException {
        try (var resourceCredentials = getResource("service_account_storage.json")) {

            var storage = StorageOptions.http()
                    .setCredentials(GoogleCredentials.fromStream(resourceCredentials))
                    .build()
                    .getService();

            var bucket = storage.get("sona_app_test");
            GoogleCloudStorageTest.storage = new GoogleCloudStorage(bucket);

        }
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
            var downloaded = storage.download(fullpath);

            assertTrue(downloaded.isPresent());

            var fileInfo = downloaded.get().getInfo();
            log.info("Downloaded file with info {}", fileInfo);

            try (var fos = new FileOutputStream("downloaded-" + fileName)) {
                IOUtils.copy(downloaded.get().getStream(), fos);
            }

            assertNotNull(fileInfo);
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void info() {

        var fileName = "test-file.webp";
        try (var resource = getResource(fileName)) {

            var fullPath = storage.store(resource, "to-info" + fileName);
            var info = storage.info(fullPath);

            assertTrue(info.isPresent());

            log.info("File info {}", info.get());

            assertNotNull(info.get());
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