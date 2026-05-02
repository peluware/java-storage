package com.peluware.storage.temp;

import com.peluware.storage.ByteRange;
import com.peluware.storage.Storage;
import com.peluware.storage.StorageUploadRef;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.peluware.storage.StorageUtils.*;

public class TempUploadService<T extends TempUploadTicket> {

    private static final int DEFAULT_CONTENT_TYPE_DETECTION_BYTES = 16384;

    protected final String tempDir;
    protected final Storage storage;
    protected final TempUploadTicketManager<T> ticketManager;
    protected final int contentTypeDetectionBytes;
    private final List<TempUploadListener<T>> listeners = new ArrayList<>();

    public TempUploadService(String tempDir, Storage storage, TempUploadTicketManager<T> ticketManager, int contentTypeDetectionBytes) {
        this.tempDir = tempDir;
        this.storage = storage;
        this.ticketManager = ticketManager;
        this.contentTypeDetectionBytes = contentTypeDetectionBytes;
    }

    public TempUploadService(Storage storage, TempUploadTicketManager<T> ticketManager) {
        this("temp", storage, ticketManager, DEFAULT_CONTENT_TYPE_DETECTION_BYTES);
    }

    public TempUploadService(String tempDir, Storage storage, TempUploadTicketManager<T> ticketManager) {
        this(tempDir, storage, ticketManager, DEFAULT_CONTENT_TYPE_DETECTION_BYTES);
    }

    public void addListener(TempUploadListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(TempUploadListener<T> listener) {
        listeners.remove(listener);
    }

    protected String generateTicket() {
        return UUID.randomUUID().toString();
    }

    protected String randomTempFileName() {
        return UUID.randomUUID().toString();
    }

    public final TempUploadTickets generateTickets(StorageUploadRef ref, Duration duration) {
        var extension = extractExtension(ref.getFileName());
        var ticket = generateTicket();

        var tempRef = ref.toBuilder()
            .directory(tempDir)
            .fileName(randomTempFileName() + extension)
            .build();

        var expiresAt = Instant.now().plus(duration);

        var uploadTicket = ticketManager.newTicket();
        uploadTicket.setTicket(ticket);
        uploadTicket.setTempPath(tempRef.getPath());
        uploadTicket.setTargetPath(ref.getPath());
        uploadTicket.setContentType(ref.getContentType());
        uploadTicket.setCreatedAt(Instant.now());
        uploadTicket.setExpiresAt(expiresAt);
        ticketManager.saveTicket(uploadTicket);

        var uploadUrl = storage.generateUploadSignedUrl(tempRef, duration);
        var deleteUrl = storage.generateDeleteSignedUrl(tempRef, duration);

        var tickets = new TempUploadTickets(uploadUrl, deleteUrl, ticket, expiresAt);
        listeners.forEach(l -> l.onTicketsGenerated(uploadTicket, tickets));
        return tickets;
    }

    public final String confirm(String ticket) throws IOException {
        var uploadTicket = ticketManager.findByTicket(ticket);

        var tempPath = uploadTicket.getTempPath();
        var targetPath = uploadTicket.getTargetPath();
        var contentType = uploadTicket.getContentType();

        if (contentType != null) {
            assertValidContentType(tempPath, targetPath, contentType);
        }

        try {
            storage.copy(tempPath, targetPath);
        } catch (StorageObjectNotFoundException e) {
            throw new TempUploadFileNotFoundException(tempPath, e);
        }

        ticketManager.deleteTicket(uploadTicket);
        listeners.forEach(l -> l.onConfirmed(uploadTicket));
        return targetPath;
    }

    protected void assertValidContentType(String tempPath, String targetPath, String contentType) throws TempUploadContentTypeMismatchException, IOException {
        var stored = storage.get(tempPath, ByteRange.first(contentTypeDetectionBytes))
            .orElseThrow(() -> new TempUploadFileNotFoundException(tempPath));

        try (InputStream content = stored.openContent()) {
            var detected = detectContentType(
                content,
                extractFilename(targetPath)
            );
            if (!detected.equals(contentType)) {
                throw new TempUploadContentTypeMismatchException(contentType, detected);
            }
        }
    }

    public void purgeExpired() throws IOException {
        var expired = ticketManager.findExpiredBefore(Instant.now());
        for (var uploadTicket : expired) {
            try {
                storage.remove(uploadTicket.getTempPath());
            } catch (StorageObjectNotFoundException ignored) {
            }
        }
        ticketManager.deleteTickets(expired);
        listeners.forEach(l -> l.onExpired(expired));
    }
}
