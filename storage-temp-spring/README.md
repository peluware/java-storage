# storage-temp-spring

Spring integration for [storage-temp](../storage-temp/README.md) that coordinates file operations with Spring transaction lifecycle.

## The problem

Storage backends (S3, GCS, disk) are not transactional. If you confirm a file upload inside a `@Transactional` method and the transaction later rolls back (e.g. a DB constraint fails), the file is already in its final location with no automatic way to undo it.

`TransactionalTempStorage` solves this by deferring file cleanup to Spring's commit/rollback hooks.

## Installation

```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>storage-temp-spring</artifactId>
    <version>1.1.1</version>
</dependency>
```

Requires `storage-temp` and `spring-tx` (both pulled transitively).

## `TransactionalTempStorage`

Drop-in replacement for `TempStorage`. Instead of moving the file directly (irreversible), it copies it to the destination and schedules cleanup via transaction hooks:

| Event | Action |
|---|---|
| **Commit** | Delete the temp file and the backup (if any) |
| **Rollback** | Restore the original file from backup (if it existed) or delete the copied file (if the destination was empty) |

```java
Storage storage = new S3Storage(s3Client, s3Presigner, "my-bucket");
TempUploadTicketManager manager = new MyTicketManager();

TransactionalTempStorage tempStorage = new TransactionalTempStorage(storage, manager);
```

Must be called inside an active Spring transaction.

## `confirmAndReplace`

For update flows where a file may already exist at the destination (e.g. replacing a user avatar), `confirmAndReplace` handles the old file atomically:

```java
// ticket == null or blank → schedule deletion of the existing file after commit
// ticket valid           → confirm upload; if the new path differs, schedule deletion of the old one
@Transactional
public String updateAvatar(String userId, @Nullable String ticket, @Nullable String currentAvatarPath) throws IOException {
    return tempStorage.confirmAndReplace(ticket, currentAvatarPath);
}
```

With validation:

```java
@Transactional
public String updateAvatar(String userId, @Nullable String ticket, @Nullable String currentPath) throws IOException {
    return tempStorage.confirmAndReplace(ticket, currentPath,
        TempStorageValidation.expectedDirectory("avatars/"),
        TempStorageValidation.maxFileSize(2 * 1024 * 1024L)
    );
}
```

## `StorageTransactionalUtils`

Utility class to schedule file deletions tied to the current transaction. Silently ignored when no transaction is active.

```java
// Delete after commit (cleanup on success)
StorageTransactionalUtils.removeAfterCommit(storage, "temp/old-file.pdf");

// Delete after rollback (cleanup on failure)
StorageTransactionalUtils.removeAfterRollback(storage, "uploads/incomplete.jpg");

// Custom completion hook
StorageTransactionalUtils.onCompletion(STATUS_ROLLED_BACK, () -> {
    // any rollback logic
});
```

Overloads accept `String...`, `Collection<String>`, and `Stream<String>`.
