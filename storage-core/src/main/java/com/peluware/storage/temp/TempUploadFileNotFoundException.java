package com.peluware.storage.temp;

public class TempUploadFileNotFoundException extends RuntimeException {

    public TempUploadFileNotFoundException(String tempPath, Throwable cause) {
        super("Temp upload file not found at path: " + tempPath, cause);
    }

    public TempUploadFileNotFoundException(String tempPath) {
        super("Temp upload file not found at path: " + tempPath);
    }
}
