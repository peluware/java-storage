package com.peluware.storage.temp;

public class TempUploadContentTypeMismatchException extends RuntimeException {

    public TempUploadContentTypeMismatchException(String expected, String detected) {
        super("Content type mismatch: expected " + expected + " but detected " + detected);
    }
}
