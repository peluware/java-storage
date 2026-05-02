package com.peluware.storage.jpa;

public class FileStoredProjection {

    private Long contentLength;
    private String contentType;
    private String originalFileName;
    private String path;

    public FileStoredProjection(Long contentLength, String contentType, String originalFileName, String path) {
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.originalFileName = originalFileName;
        this.path = path;
    }

    public FileStoredProjection() {
    }

    public Long getContentLength() {
        return this.contentLength;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getOriginalFileName() {
        return this.originalFileName;
    }

    public String getPath() {
        return this.path;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setPath(String path) {
        this.path = path;
    }
}