package com.peluware.storage.temp;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public interface TempUploadTicket {

    String getTicket();

    void setTicket(String ticket);

    Instant getCreatedAt();

    void setCreatedAt(Instant createdAt);

    String getTargetPath();

    void setTargetPath(String targetPath);

    String getTempPath();

    void setTempPath(String tempPath);

    Instant getExpiresAt();

    void setExpiresAt(Instant expiresAt);

    @Nullable String getContentType();

    void setContentType(@Nullable String contentType);

}
