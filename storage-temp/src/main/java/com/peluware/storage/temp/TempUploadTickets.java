package com.peluware.storage.temp;

import java.net.URL;
import java.time.Instant;
import java.util.Objects;

/**
 * Resultado de {@link TempStorage#generateTickets}: contiene las URLs firmadas para que
 * el cliente suba y/o elimine el archivo directamente en el backend de storage,
 * junto con el ticket necesario para confirmar la subida posteriormente.
 */
public class TempUploadTickets {
    private URL uploadUrl;
    private URL deleteUrl;
    private String ticket;
    private Instant expiresAt;

    public TempUploadTickets(URL uploadUrl, URL deleteUrl, String ticket, Instant expiresAt) {
        this.uploadUrl = uploadUrl;
        this.deleteUrl = deleteUrl;
        this.ticket = ticket;
        this.expiresAt = expiresAt;
    }

    /** URL firmada para que el cliente suba el archivo directamente al backend. */
    public URL getUploadUrl() {
        return this.uploadUrl;
    }

    /** URL firmada para que el cliente elimine el archivo temporal si cancela la operación. */
    public URL getDeleteUrl() {
        return this.deleteUrl;
    }

    /** Identificador del ticket que debe presentarse al llamar a {@link TempStorage#confirm}. */
    public String getTicket() {
        return this.ticket;
    }

    /** Instante hasta el cual el ticket y las URLs firmadas son válidos. */
    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    public void setUploadUrl(URL uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public void setDeleteUrl(URL deleteUrl) {
        this.deleteUrl = deleteUrl;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TempUploadTickets t)) return false;
        return Objects.equals(ticket, t.ticket);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ticket);
    }

    @Override
    public String toString() {
        return "TempUploadTickets(uploadUrl=" + uploadUrl + ", deleteUrl=" + deleteUrl + ", ticket=" + ticket + ", expiresAt=" + expiresAt + ")";
    }
}
