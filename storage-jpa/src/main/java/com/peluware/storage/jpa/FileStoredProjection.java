package com.peluware.storage.jpa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileStoredProjection {

    private Long contentLength;
    private String contentType;
    private String originalFileName;
    private String path;
}