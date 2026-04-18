package com.peluware.storage;

import lombok.experimental.UtilityClass;
import org.apache.tika.Tika;

import java.io.InputStream;


@UtilityClass
public final class StorageUtils {

    private static final Tika TIKA = new Tika();


    /**
     * Obtiene la ruta ideal de un directorio para almacenar un archivo
     *
     * @param directory Directorio a normalizar
     * @return Directorio normalizado
     */
    public static String normalizeDirectory(String directory) {
        if (directory.isBlank() || directory.equals("/")) return "";
        var normalizedDir = directory.startsWith("/") ? directory.substring(1) : directory;
        return normalizedDir.endsWith("/") ? normalizedDir.substring(0, normalizedDir.length() - 1) : normalizedDir;
    }

    public static String buildPath(String path, String filename) {
        return path.isEmpty() ? filename : path + "/" + filename;
    }

    /**
     * Construye un archivo descargado con los bytes, tamaño, nombre, ruta y tipo de contenido
     *
     * @param stream Flujo del archivo
     * @param fileSize Tamaño del archivo
     * @param filename Nombre del archivo
     * @param directory Directorio del archivo
     * @param contentType Tipo de contenido
     * @return Archivo descargado
     */
    public static Stored constructStoredFile(InputStream stream, long fileSize, String filename, String directory, String contentType) {
        return Stored.builder()
                .stream(stream)
                .info(constructFileInfo(filename, fileSize, directory, contentType))
                .build();
    }


    /**
     * Construye un archivo descargado con los bytes, tamaño, nombre y ruta, el tipo de contenido se detecta automáticamente con el nombre del archivo
     * @param stream Flujo del archivo
     * @param fileSize Tamaño del archivo
     * @param filename Nombre del archivo
     * @param directory Directorio del archivo
     * @return Archivo descargado
     */
    public static Stored constructStoredFile(InputStream stream, long fileSize, String filename, String directory) {
        return constructStoredFile(stream, fileSize, filename, directory, guessContentType(filename));
    }

    /**
     * Construye la información de un archivo almacenado con el nombre, tamaño y ruta, el tipo de contenido se detecta automáticamente con el nombre del archivo
     * @param filename Nombre del archivo
     * @param fileSize Tamaño del archivo
     * @param directory Directorio del archivo
     * @return Información del archivo almacenado
     */
    public static Stored.Info constructFileInfo(String filename, long fileSize, String directory) {
        return constructFileInfo(filename, fileSize, directory, guessContentType(filename));
    }

    /**
     * Construye la información de un archivo almacenado con el nombre, tamaño, ruta y tipo de contenido
     * @param filename Nombre del archivo
     * @param fileSize Tamaño del archivo
     * @param directory Directorio del archivo
     * @param contentType Tipo de contenido
     * @return Información del archivo almacenado
     */
    public static Stored.Info constructFileInfo(String filename, long fileSize, String directory, String contentType) {

        return Stored.Info.builder()
                .fileName(filename)
                .fileSize(fileSize)
                .directory(directory)
                .contentType(contentType)
                .build();

    }

    /**
     * Detecta el tipo de contenido de un archivo a partir de su nombre
     * @param filename Nombre del archivo
     * @return Tipo de contenido
     */
    public static String guessContentType(String filename) {
        return TIKA.detect(filename);
    }
}
