# Storage

[![Maven Central](https://img.shields.io/maven-central/v/com.peluware/storage)](https://central.sonatype.com/artifact/com.peluware/storage)

A framework-agnostic Java 21 library for storing files across multiple backends through a unified API.

## Modules

| Module                  | Artifact                | Backend                      |
|-------------------------|-------------------------|------------------------------|
| `storage-core`          | `storage-core`          | Abstract API, decorators     |
| `storage-disk`          | `storage-disk`          | Local filesystem             |
| `storage-s3`            | `storage-s3`            | Amazon S3 (AWS SDK v2)       |
| `storage-google-cloud`  | `storage-google-cloud`  | Google Cloud Storage         |
| `storage-jpa`           | `storage-jpa`           | Relational DB via JPA        |
| `storage-spring-gridfs` | `storage-spring-gridfs` | MongoDB GridFS (Spring Data) |

## Installation

Add only the module(s) you need. All of them pull `storage-core` transitively.

```xml
<!-- Amazon S3 -->
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>storage-s3</artifactId>
    <version>1.0.6</version>
</dependency>

<!-- Google Cloud Storage -->
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>storage-google-cloud</artifactId>
    <version>1.0.6</version>
</dependency>

<!-- Local disk -->
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>storage-disk</artifactId>
    <version>1.0.6</version>
</dependency>

<!-- JPA (relational DB) -->
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>storage-jpa</artifactId>
    <version>1.0.6</version>
</dependency>

<!-- MongoDB GridFS (requires Spring Data MongoDB) -->
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>storage-spring-gridfs</artifactId>
    <version>1.0.6</version>
</dependency>
```

## Quick start

```java
// Disk
Storage storage = new DiskStorage("/var/uploads");

// S3
Storage storage = new S3Storage(s3Client, "my-bucket");

// S3 with presigned URLs
Storage storage = new S3Storage(s3Client, s3Presigner, "my-bucket");

// Google Cloud Storage
Storage storage = new GoogleCloudStorage(bucket);

// JPA
Storage storage = new JpaStorage(entityManager);

// GridFS (Spring)
Storage storage = new GridFSStorage(gridFsTemplate, gridFsOperations);
```

## API

### Store

```java
// Store from InputStream or byte[]
String path = storage.store(inputStream, "photo.jpg");
String path = storage.store(bytes, "photo.jpg", "avatars/");

// Store from StorageObject (explicit metadata)
String path = storage.store(new StorageObject("avatars/", "photo.jpg", inputStream));

// Atomic multi-file store — rolls back already-StoredObjectObject files on failure
storage.store(obj1, obj2, obj3);
```

### Get & check existence

```java
Optional<StoredObject> file = storage.get("avatars/photo.jpg");
Optional<StoredObject> file = storage.get("photo.jpg", "avatars/");

boolean exists = storage.exists("avatars/photo.jpg");

// Partial content (byte range)
Optional<StoredObject> chunk = storage.get("video.mp4", ByteRange.of(0, 1_048_575)); // first 1 MB
```

### Read content

```java
StoredObject file = storage.get("avatars/photo.jpg").orElseThrow();

file.getFileName();      // "photo.jpg"
file.getDirectory();     // "avatars/"
file.getContentLength();
file.getContentType();

try (InputStream in = file.openContent()) {
    // read...
}
```

### Remove

```java
storage.remove("avatars/photo.jpg");
storage.remove("photo.jpg", "avatars/");
```

### List

```java
List<StoredObject> files = storage.list("avatars/");
List<StoredObject> all   = storage.list();
```

### Transfer between backends

```java
Storage disk = new DiskStorage("/tmp");
Storage s3   = new S3Storage(client, "my-bucket");

disk.transferTo(s3, "documents/report.pdf");
```

### Purge

Implement `PurgableStoredObject` on your domain entities to delete their associated files in one call:

```java
public class UserProfile implements PurgableStoredObject {
    private String avatarPath;

    @Override
    public List<String> filesFullPaths() {
        return List.of(avatarPath);
    }
}

storage.purge(userProfile);
storage.purge(listOfProfiles);
```

### Signed URLs

```java
// Download URL valid for 15 minutes
URL url = storage.generateDownloadSignedUrl("documents/report.pdf", Duration.ofMinutes(15));

// Download URL for a byte range
URL url = storage.generateDownloadSignedUrl("video.mp4", Duration.ofMinutes(10), ByteRange.of(0, 999));

// Upload URL
URL url = storage.generateUploadSignedUrl("uploads/photo.jpg", Duration.ofMinutes(5));

// Upload URL with content constraints
StorageUploadRef ref = new StorageUploadRef("uploads/", "photo.jpg", "image/jpeg", 204800L);
URL url = storage.generateUploadSignedUrl(ref, Duration.ofMinutes(5));
```

> Signed URLs are supported by `S3Storage` and `GoogleCloudStorage`. Other backends throw `UnsupportedOperationException`.

## Decorators

### ScopedStorage

Confines all operations to a base directory. Useful for isolating tenants or users.

```java
// Static scope
Storage userStorage = new ScopedStorage(storage, "users/42/");

// Dynamic scope (resolved on each operation)
Storage tenantStorage = new ScopedStorage(storage, () -> "tenants/" + TenantContext.get() + "/");

userStorage.store(inputStream, "avatar.jpg"); // StoredObjectObject at users/42/avatar.jpg
```

### DelegatingStorage

Wraps another `Storage` instance and delegates all operations. Extend it to intercept or decorate behavior.

```java
// Simple wrapping
Storage delegating = DelegatingStorage.of(storage);
Storage delegating = DelegatingStorage.of(() -> resolveStorage());

// Custom behavior via extension
public class AuditingStorage extends DelegatingStorage {
    @Override
    protected Storage getDelegate() { return delegate; }

    @Override
    public String store(StorageObject obj) throws IOException {
        var path = super.store(obj);
        auditLog.record("StoredObjectObject", path);
        return path;
    }
}
```

## Lifecycle

`Storage` implements `AutoCloseable`. Backends that hold client connections (`S3Storage`, `GoogleCloudStorage`) release them on `close()`. Decorators (`ScopedStorage`, `DelegatingStorage`) propagate the call to the delegate.

```java
try (Storage storage = new S3Storage(s3Client, "my-bucket")) {
    storage.store(inputStream, "file.txt");
}
```

When managed by a DI container (Spring, CDI, etc.), bind the lifecycle to the container instead of using try-with-resources.

## Requirements

- Java 21+
- Maven 3.6+

## License

Apache License 2.0
