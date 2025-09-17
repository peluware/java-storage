package com.peluware.storage.springframework.data.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
    private String path;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] content;
}