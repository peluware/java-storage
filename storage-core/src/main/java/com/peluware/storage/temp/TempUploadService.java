package com.peluware.storage.temp;

import com.peluware.storage.ByteRange;
import com.peluware.storage.Storage;
import com.peluware.storage.StorageUploadRef;
import com.peluware.storage.StorageUtils;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class TempUploadService<T extends TempUploadTicket> {

    private static final int DEFAULT_CONTENT_TYPE_DETECTION_BYTES = 4096;

    private final String tempDir;
    private final Storage storage;
    private final TempUploadTicketManager<T> ticketManager;
    private final int contentTypeDetectionBytes;
    private final List<TempUploadListener<T>> listeners = new ArrayList<>();

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

    public TempUploadTickets generateTickets(StorageUploadRef ref, Duration duration) {
        var extension = StorageUtils.extractExtension(ref.getFileName());
        var ticket = generateTicket();

        var tempRef = ref.toBuilder()
            .directory(tempDir)
            .fileName(randomTempFileName() + extension)
            .build();

        var ticketRef = ticketManager.newTicket();
        ticketRef.setTicket(ticket);
        ticketRef.setTempPath(tempRef.getPath());
        ticketRef.setTargetPath(ref.getPath());
        ticketRef.setContentType(ref.getContentType());
        ticketRef.setCreatedAt(Instant.now());

        var expiresAt = Instant.now().plus(duration);
        var uploadUrl = storage.generateUploadSignedUrl(tempRef, duration);
        var deleteUrl = storage.generateDeleteSignedUrl(tempRef, duration);

        var tickets = new TempUploadTickets(uploadUrl, deleteUrl, ticket, expiresAt);
        listeners.forEach(l -> l.onTicketsGenerated(ticketRef, tickets));
        return tickets;
    }

    public String confirm(String ticket) throws IOException {
        var ticketRef = ticketManager.findByTicket(ticket);

        var tempPath = ticketRef.getTempPath();
        var targetPath = ticketRef.getTargetPath();

        var contentType = ticketRef.getContentType();

        if (contentType != null) {
            var stored = storage.get(tempPath, ByteRange.first(contentTypeDetectionBytes))
                .orElseThrow(() -> new TempUploadFileNotFoundException(tempPath));

            try (InputStream content = stored.openContent()) {
                var detected = StorageUtils.detectContentType(content.readAllBytes());
                if (!detected.equals(contentType)) {
                    throw new TempUploadContentTypeMismatchException(contentType, detected);
                }
            }
        }

        try {
            storage.copy(tempPath, targetPath);
        } catch (StorageObjectNotFoundException e) {
            throw new TempUploadFileNotFoundException(tempPath, e);
        }

        ticketManager.deleteTicket(ticketRef);
        listeners.forEach(l -> l.onConfirmed(ticketRef));
        return targetPath;
    }
}
