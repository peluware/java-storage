package com.peluware.storage.temp;

public class TempUploadTicketNotFoundException extends RuntimeException {

    public TempUploadTicketNotFoundException(String ticket) {
        super("Temp upload ticket not found: " + ticket);
    }

    public TempUploadTicketNotFoundException(String ticket, Throwable cause) {
        super("Temp upload ticket not found: " + ticket, cause);
    }
}
