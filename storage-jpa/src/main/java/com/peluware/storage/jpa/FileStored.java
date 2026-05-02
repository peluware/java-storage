package com.peluware.storage.jpa;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "file_stored")
public class FileStored {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_length", nullable = false)
    private Long contentLength;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String directory;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Lob
    private byte @Nullable [] content;

    public FileStored(UUID id, Long contentLength, String contentType, String originalFileName, String directory, LocalDateTime uploadedAt, byte @Nullable [] content) {
        this.id = id;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.originalFileName = originalFileName;
        this.directory = directory;
        this.uploadedAt = uploadedAt;
        this.content = content;
    }

    public FileStored() {
    }

    public static FileStoredBuilder builder() {
        return new FileStoredBuilder();
    }

    public UUID getId() {
        return this.id;
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

    public String getDirectory() {
        return this.directory;
    }

    public LocalDateTime getUploadedAt() {
        return this.uploadedAt;
    }

    public byte @Nullable [] getContent() {
        return this.content;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public void setContent(byte @Nullable [] content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileStored f)) return false;
        return Objects.equals(id, f.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String toString() {
        return "FileStored(id=" + this.getId() + ", contentLength=" + this.getContentLength() + ", contentType=" + this.getContentType() + ", originalFileName=" + this.getOriginalFileName() + ", directory=" + this.getDirectory() + ", uploadedAt=" + this.getUploadedAt() + ", content=" + java.util.Arrays.toString(this.getContent()) + ")";
    }

    public static class FileStoredBuilder {
        private UUID id;
        private Long contentLength;
        private String contentType;
        private String originalFileName;
        private String directory;
        private LocalDateTime uploadedAt;
        private byte @Nullable [] content;

        FileStoredBuilder() {
        }

        public FileStoredBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public FileStoredBuilder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public FileStoredBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public FileStoredBuilder originalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
            return this;
        }

        public FileStoredBuilder directory(String directory) {
            this.directory = directory;
            return this;
        }

        public FileStoredBuilder uploadedAt(LocalDateTime uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public FileStoredBuilder content(byte @Nullable [] content) {
            this.content = content;
            return this;
        }

        public FileStored build() {
            return new FileStored(this.id, this.contentLength, this.contentType, this.originalFileName, this.directory, this.uploadedAt, this.content);
        }
    }
}