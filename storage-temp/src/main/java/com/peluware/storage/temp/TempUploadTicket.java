package com.peluware.storage.temp;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Representa un ticket de subida temporal pendiente de confirmación.
 * <p>
 * Contiene la información necesaria para rastrear el archivo subido al directorio
 * temporal y moverlo a su destino final cuando se invoque {@link TempStorage#confirm}.
 */
public interface TempUploadTicket {

    /** Identificador único del ticket, generado al momento de la subida. */
    String getTicket();

    /** Instante en que fue creado el ticket. */
    Instant getCreatedAt();

    /** Ruta final donde debe quedar el archivo una vez confirmado. */
    String getTargetPath();

    /** Ruta temporal donde el cliente subió el archivo. */
    String getTempPath();

    /** Instante a partir del cual el ticket se considera expirado. */
    Instant getExpiresAt();

    /**
     * Content type esperado del archivo, usado para validar el contenido real
     * mediante detección de magic bytes durante la confirmación.
     * Si es {@code null}, la validación de content type se omite.
     */
    @Nullable String getContentType();

}
