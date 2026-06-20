package com.peluware.storage.temp;

import com.peluware.storage.Storage;
import com.peluware.storage.StorageUtils;

import java.io.IOException;

/**
 * Estrategia de validación aplicada sobre un {@link TempUploadTicket} y el {@link Storage}
 * durante la confirmación.
 * <p>
 * Es una interfaz funcional: puede implementarse con una lambda o combinarse mediante
 * los métodos de fábrica estáticos {@link #composite}, {@link #valid}, {@link #expectedDirectory},
 * {@link #expectedPath}, {@link #expectedExtension} y {@link #maxFileSize}.
 */
@FunctionalInterface
public interface TempStorageValidation {

    /**
     * Valida el ticket. Lanza excepción si la validación falla.
     *
     * @param ticket  ticket a validar
     * @param storage backend de storage, disponible para validaciones que requieran
     *                inspeccionar el archivo temporal
     * @throws TempStorageValidationException si el ticket no cumple la validación
     * @throws IOException                    si ocurre un error de I/O durante la validación
     */
    void validate(TempUploadTicket ticket, Storage storage) throws TempStorageValidationException, IOException;

    /**
     * Validación vacía que siempre pasa. Útil como valor por defecto.
     */
    static TempStorageValidation valid() {
        return (ticket, storage) -> {
        };
    }

    /**
     * Combina múltiples validaciones en una sola que las ejecuta en orden.
     * Falla en la primera validación que lance excepción.
     *
     * @param validations validaciones a combinar
     */
    static TempStorageValidation composite(TempStorageValidation... validations) {
        return (ticket, storage) -> {
            for (var validation : validations) {
                validation.validate(ticket, storage);
            }
        };
    }

    /**
     * Valida que el directorio del {@code targetPath} del ticket coincida con el esperado.
     * El directorio se extrae mediante {@link StorageUtils#extractDirectory}.
     * <p>
     * Más seguro que comparar prefijos de string: {@code "invoices-old/f.pdf"} no pasaría
     * una validación de directorio {@code "invoices"}.
     *
     * @param expectedDirectory directorio esperado
     * @throws TempStorageValidationException si el directorio no coincide
     */
    static TempStorageValidation expectedDirectory(String expectedDirectory) {
        return (ticket, storage) -> {
            var actual = StorageUtils.extractDirectory(ticket.getTargetPath());
            if (!actual.equals(expectedDirectory)) {
                throw new TempStorageValidationException(
                    "Expected target directory '" + expectedDirectory + "' but was '" + actual + "'"
                );
            }
        };
    }

    /**
     * Valida que el {@code targetPath} del ticket sea exactamente la ruta esperada.
     * Previene que un ticket generado para una ruta se use para confirmar otra.
     *
     * @param expectedPath ruta completa esperada
     * @throws TempStorageValidationException si la ruta no coincide
     */
    static TempStorageValidation expectedPath(String expectedPath) {
        return (ticket, storage) -> {
            var actual = ticket.getTargetPath();
            if (!actual.equals(expectedPath)) {
                throw new TempStorageValidationException(
                    "Expected target path '" + expectedPath + "' but was '" + actual + "'"
                );
            }
        };
    }

    /**
     * Valida que la extensión del {@code targetPath} sea una de las permitidas.
     * La comparación es insensible a mayúsculas.
     * <p>
     * Útil como validación ligera previa a la detección por magic bytes.
     *
     * @param extensions extensiones permitidas, con o sin punto (ej. {@code ".pdf"} o {@code "pdf"})
     * @throws TempStorageValidationException si la extensión no está entre las permitidas
     */
    static TempStorageValidation expectedExtension(String... extensions) {
        return (ticket, storage) -> {
            var actual = StorageUtils.extractExtension(ticket.getTargetPath()).toLowerCase();
            for (var ext : extensions) {
                var normalized = ext.startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase();
                if (actual.equals(normalized)) return;
            }
            throw new TempStorageValidationException(
                "File extension '" + actual + "' is not allowed for path '" + ticket.getTargetPath() + "'"
            );
        };
    }

    /**
     * Valida que el nombre de archivo del {@code targetPath} sea exactamente el esperado.
     * La comparación es sensible a mayúsculas.
     * <p>
     * Complementa a {@link #expectedDirectory}: úsalos juntos cuando necesites controlar
     * tanto el directorio como el nombre, sin fijar la ruta completa.
     *
     * @param expectedFileName nombre de archivo esperado, incluyendo extensión (ej. {@code "avatar.jpg"})
     * @throws TempStorageValidationException si el nombre no coincide
     */
    static TempStorageValidation expectedFileName(String expectedFileName) {
        return (ticket, storage) -> {
            var actual = StorageUtils.extractFilename(ticket.getTargetPath());
            if (!actual.equals(expectedFileName)) {
                throw new TempStorageValidationException(
                    "Expected file name '" + expectedFileName + "' but was '" + actual + "'"
                );
            }
        };
    }

    /**
     * Valida que el nombre de archivo del {@code targetPath}, sin extensión, sea exactamente el esperado.
     * La comparación es sensible a mayúsculas.
     * <p>
     * Útil cuando el slot de destino tiene un nombre fijo, pero el formato puede variar
     * (ej. {@code "avatar"} permite tanto {@code "avatar.jpg"} como {@code "avatar.webp"}).
     *
     * @param expectedBaseName nombre base esperado, sin extensión (ej. {@code "avatar"})
     * @throws TempStorageValidationException si el nombre base no coincide
     */
    static TempStorageValidation expectedBaseName(String expectedBaseName) {
        return (ticket, storage) -> {
            var actual = StorageUtils.extractBaseName(ticket.getTargetPath());
            if (!actual.equals(expectedBaseName)) {
                throw new TempStorageValidationException(
                    "Expected file base name '" + expectedBaseName + "' but was '" + actual + "'"
                );
            }
        };
    }

    /**
     * Valida que el tamaño del archivo temporal no supere el límite indicado.
     * Consulta el tamaño real desde el storage sin descargar el contenido.
     *
     * @param maxBytes tamaño máximo permitido en bytes
     * @throws TempStorageValidationException  si el archivo supera el límite
     * @throws TempUploadFileNotFoundException si el archivo temporal no existe
     */
    static TempStorageValidation maxFileSize(long maxBytes) {
        return (ticket, storage) -> {
            var stored = storage.get(ticket.getTempPath()).orElseThrow(() -> new TempUploadFileNotFoundException(ticket.getTempPath()));
            if (stored.getFileSize() > maxBytes) {
                throw new TempStorageValidationException(
                    "File size " + stored.getFileSize() + " bytes exceeds maximum of " + maxBytes + " bytes"
                );
            }
        };
    }
}
