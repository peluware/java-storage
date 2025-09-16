package com.peluware.storage;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

/**
 * Representa un archivo descargado
 */
@Data
@Builder
public class Stored {

    private InputStream stream;
    private Info info;

    /**
     * Representa la información de un archivo almacenado
     */
    @Data
    @Builder
    public static class Info {
        private String path;
        private String filename;
        private String contentType;
        private Long filesize;
    }
}