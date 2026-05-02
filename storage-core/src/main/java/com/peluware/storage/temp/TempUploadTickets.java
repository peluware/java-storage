package com.peluware.storage.temp;

import java.net.URL;
import java.time.Instant;
import java.util.Objects;

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

    public URL getUploadUrl() {
        return this.uploadUrl;
    }

    public URL getDeleteUrl() {
        return this.deleteUrl;
    }

    public String getTicket() {
        return this.ticket;
    }

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

    public String toString() {
        return "TempUploadTickets(uploadUrl=" + this.getUploadUrl() + ", deleteUrl=" + this.getDeleteUrl() + ", ticket=" + this.getTicket() + ", expiresAt=" + this.getExpiresAt() + ")";
    }
}
