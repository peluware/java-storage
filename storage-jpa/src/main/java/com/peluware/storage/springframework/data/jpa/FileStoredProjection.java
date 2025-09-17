package com.peluware.storage.springframework.data.jpa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor // Constructor con todos los campos (necesario para ConstructorResult)
@NoArgsConstructor  // Opcional
public class FileStoredProjection {

    private Long contentLength;
    private String contentType;
    private String originalFileName;
    private String path;
}