# storage-temp

Two-step (claim-check) temporary file upload module for the [PeluWare Storage](../README.md) library.

## How it works

1. The backend calls `generateTickets()` to reserve a temporary slot and obtain signed URLs.
2. The client uploads the file **directly** to the storage backend using the signed upload URL — no backend proxy involved.
3. The client presents the ticket to the backend, which calls `confirm()` to move the file to its final destination.

```
Client                Backend              Storage (S3/GCS/…)
  |                      |                        |
  |-- POST /upload ----→ |                        |
  |                      |-- generateTickets() -→ |
  |                      |←-- {uploadUrl, ticket} |
  |←-- {uploadUrl, ticket} ---------------------- |
  |                      |                        |
  |-- PUT uploadUrl --→  |                        |  (direct upload)
  |←-- 200 OK --------→ |                        |
  |                      |                        |
  |-- POST /confirm --→  |                        |
  |                      |-- confirm(ticket) ---→ |
  |                      |←-- targetPath -------- |
  |←-- 200 OK --------→ |                        |
```

## Installation

```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>storage-temp</artifactId>
    <version>1.1.0</version>
</dependency>
```

Requires `storage-core` (pulled transitively) and [Apache Tika](https://tika.apache.org/) for magic-bytes content type detection.

## Usage

### 1. Implement `TempUploadTicketManager`

Persist tickets wherever fits your stack (JPA, Redis, MongoDB, etc.):

```java
public class MyTicketManager implements TempUploadTicketManager {

    @Override
    public TempUploadTicket newTicket() { /* build and return a new ticket instance */ }

    @Override
    public void saveTicket(TempUploadTicket ticket) { /* persist */ }

    @Override
    public TempUploadTicket findByTicket(String ticket) throws TempUploadTicketNotFoundException {
        return repo.findByTicket(ticket)
            .orElseThrow(() -> new TempUploadTicketNotFoundException(ticket));
    }

    @Override
    public List<? extends TempUploadTicket> findExpiredBefore(Instant instant) {
        return repo.findByExpiresAtBefore(instant);
    }

    @Override
    public void deleteTicket(TempUploadTicket ticket) { repo.delete(ticket); }

    @Override
    public void deleteTickets(List<? extends TempUploadTicket> tickets) { repo.deleteAll(tickets); }
}
```

### 2. Create `TempStorage`

```java
Storage storage = new S3Storage(s3Client, s3Presigner, "my-bucket");
TempUploadTicketManager manager = new MyTicketManager();

TempStorage tempStorage = new TempStorage("temp", storage, manager);
```

### 3. Generate tickets

```java
StorageUploadRef ref = StorageUploadRef.builder()
    .directory("invoices/")
    .fileName("invoice.pdf")
    .contentType("application/pdf")
    .contentLength(204800L)
    .build();

TempUploadTickets result = tempStorage.generateTickets(ref, Duration.ofMinutes(15));

result.getUploadUrl();  // signed URL for the client to PUT the file
result.getDeleteUrl();  // signed URL to cancel the upload
result.getTicket();     // opaque token to present at confirm time
result.getExpiresAt();  // expiration instant
```

### 4. Confirm the upload

```java
// No extra validation
String finalPath = tempStorage.confirm(ticket);

// With validation
String finalPath = tempStorage.confirm(ticket,
    TempStorageValidation.expectedDirectory("invoices/"),
    TempStorageValidation.maxFileSize(5 * 1024 * 1024L)
);
```

## Validations

`TempStorageValidation` is a `@FunctionalInterface` with built-in static factories:

| Factory | Description |
|---|---|
| `valid()` | No-op, always passes |
| `composite(v1, v2, …)` | Runs validations in order, fails on first error |
| `expectedDirectory(dir)` | Target path must be in the given directory |
| `expectedPath(path)` | Target path must match exactly |
| `expectedExtension(exts…)` | Target file extension must be one of the allowed values |
| `maxFileSize(bytes)` | File size must not exceed the limit (reads metadata, not content) |

Custom validations are lambdas:

```java
TempStorageValidation onlyForUser = (ticket, storage) -> {
    if (!ticket.getTargetPath().startsWith("users/" + userId + "/")) {
        throw new TempUploadTicketValidationEception("Unauthorized target path");
    }
};

tempStorage.confirm(ticket, onlyForUser);
```

## Content type validation

When the `StorageUploadRef` declares a `contentType`, `TempStorage` automatically validates it during `confirm()` by reading the first bytes of the uploaded file and detecting the actual MIME type via Apache Tika (magic-bytes detection). This is a server-side fallback — the storage backend (S3, GCS) already enforces the declared content type at the HTTP layer.

The default detection window is **16 384 bytes**. It can be changed via the constructor:

```java
new TempStorage("temp", storage, manager, 4096);
```

## Purge expired tickets

Call `purgeExpired()` from a scheduled job to remove stale tickets and their temporary files:

```java
@Scheduled(fixedDelay = "PT1H")
public void purge() throws IOException {
    tempStorage.purgeExpired();
}
```
