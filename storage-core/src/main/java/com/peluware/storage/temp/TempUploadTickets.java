package com.peluware.storage.temp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URL;
import java.time.Instant;

@Data
@AllArgsConstructor
public class TempUploadTickets {
    private URL uploadUrl;
    private URL deleteUrl;
    private String ticket;
    private Instant expiresAt;
}
