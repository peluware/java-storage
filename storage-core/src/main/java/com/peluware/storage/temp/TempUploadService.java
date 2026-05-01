package com.peluware.storage.temp;

import com.peluware.storage.Storage;
import com.peluware.storage.StorageUploadRef;
import com.peluware.storage.StorageUtils;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class TempUploadService<T extends TempUploadTicket> {

    private final String tempDir;
    private final Storage storage;
    private final TempUploadTicketManager<T> ticketManager;
    private final List<TempUploadListener<T>> listeners = new ArrayList<>();

    public TempUploadService(Storage storage, TempUploadTicketManager<T> ticketManager) {
        this("temp", storage, ticketManager);
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
