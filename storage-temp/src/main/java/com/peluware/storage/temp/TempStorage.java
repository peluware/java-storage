package com.peluware.storage.temp;

import com.peluware.storage.ByteRange;
import com.peluware.storage.Storage;
import com.peluware.storage.StorageUploadRef;
import com.peluware.storage.exceptions.StorageObjectNotFoundException;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.peluware.storage.StorageUtils.*;

/**
 * Servicio de subida temporal de archivos con confirmación en dos pasos.
 * <p>
 * El flujo es:
 * <ol>
 *   <li>El cliente llama a {@link #generateTickets} para obtener URL firmadas y un ticket.</li>
 *   <li>El cliente sube el archivo directamente al backend usando la URL de subida.</li>
 *   <li>El cliente llama a {@link #confirm} con el ticket para mover el archivo a su destino final.</li>
 * </ol>
 * El archivo se sube inicialmente al directorio temporal ({@code tempDir}) con un nombre
 * aleatorio para evitar colisiones. Al confirmar, se mueve a la ruta destino indicada
 * en el {@link StorageUploadRef} original.
 */
public class TempStorage {

    static final int DEFAULT_CONTENT_TYPE_DETECTION_BYTES = 16384;

    protected final String tempDir;
    protected final Storage storage;
    protected final TempUploadTicketManager ticketManager;
    protected final int contentTypeDetectionBytes;

    public TempStorage(String tempDir, Storage storage, TempUploadTicketManager ticketManager, int contentTypeDetectionBytes) {
        this.tempDir = tempDir;
        this.storage = storage;
        this.ticketManager = ticketManager;
        this.contentTypeDetectionBytes = contentTypeDetectionBytes;
    }

    public TempStorage(Storage storage, TempUploadTicketManager ticketManager) {
        this("temp", storage, ticketManager, DEFAULT_CONTENT_TYPE_DETECTION_BYTES);
    }

    public TempStorage(String tempDir, Storage storage, TempUploadTicketManager ticketManager) {
        this(tempDir, storage, ticketManager, DEFAULT_CONTENT_TYPE_DETECTION_BYTES);
    }

    /**
     * Genera el identificador único del ticket. Puede sobreescribirse para cambiar la estrategia.
     */
    protected String generateTicket() {
        return UUID.randomUUID().toString();
    }

    /**
     * Genera el nombre aleatorio del archivo temporal. Puede sobreescribirse para cambiar la estrategia.
     */
    protected String randomTempFileName() {
        return UUID.randomUUID().toString();
    }

    /**
     * Genera un ticket de subida temporal con las URL firmadas para subir y eliminar el archivo.
     * El {@code purgeDuration} se calcula automáticamente como {@code urlDuration + 4 horas},
     * dando margen al usuario para completar y enviar el formulario sin re-subir el archivo.
     *
     * @param ref         referencia con el nombre, directorio destino y metadatos opcionales del archivo
     * @param urlDuration tiempo de validez de las URL's firmadas para PUT/DELETE
     * @return {@link TempUploadTickets} con las URL firmadas, el ticket y la fecha de expiración de las URL's
     */
    public TempUploadTickets generateTickets(StorageUploadRef ref, Duration urlDuration) {
        return generateTickets(ref, urlDuration, urlDuration.plus(Duration.ofHours(4)));
    }

    /**
     * Genera un ticket de subida temporal con tiempos desacoplados para las URL's firmadas y la purga.
     * <p>
     * {@code urlDuration} debe ser corto (minutos): cubre solo el momento de la subida directa al backend.
     * {@code purgeDuration} puede ser largo (horas): da margen al usuario para confirmar desde el formulario
     * sin obligarlo a re-subir el archivo si se demoró.
     *
     * @param ref          referencia con el nombre, directorio destino y metadatos opcionales del archivo
     * @param urlDuration  tiempo de validez de las URL's firmadas para PUT/DELETE
     * @param purgeDuration tiempo tras el cual el temporal puede ser eliminado por {@link #purgeExpired}
     * @return {@link TempUploadTickets} con las URL firmadas, el ticket y la fecha de expiración de las URL's
     */
    public TempUploadTickets generateTickets(StorageUploadRef ref, Duration urlDuration, Duration purgeDuration) {
        var extension = extractExtension(ref.getFileName());
        var ticket = generateTicket();

        var tempRef = ref.toBuilder()
            .directory(tempDir)
            .fileName(randomTempFileName() + extension)
            .build();

        var uploadTicket = newTempUploadTicket(ref, tempRef, ticket, urlDuration, purgeDuration);

        ticketManager.saveTicket(uploadTicket);

        var uploadUrl = storage.generateUploadSignedUrl(tempRef, urlDuration);
        var deleteUrl = storage.generateDeleteSignedUrl(tempRef, urlDuration);

        return new TempUploadTickets(uploadUrl, deleteUrl, ticket, uploadTicket.getExpiresAt());
    }

    /**
     * Confirma la subida sin validación adicional.
     *
     * @param ticket identificador del ticket
     * @return ruta final donde quedó almacenado el archivo
     */
    public final String confirm(String ticket) throws IOException, TempUploadTicketNotFoundException {
        return confirm(ticket, TempStorageValidation.valid());
    }

    /**
     * Confirma la subida aplicando múltiples validaciones en orden.
     *
     * @param ticket      identificador del ticket
     * @param validations validaciones a aplicar antes de mover el archivo
     * @return ruta final donde quedó almacenado el archivo
     */
    public final String confirm(String ticket, TempStorageValidation... validations) throws IOException, TempUploadTicketNotFoundException, TempStorageValidationException {
        return confirm(ticket, TempStorageValidation.composite(validations));
    }

    /**
     * Confirma la subida aplicando una validación y mueve el archivo al destino final.
     * Si el ticket tiene {@code contentType} definido, valida el contenido real del archivo
     * leyendo los primeros {@link #contentTypeDetectionBytes} bytes.
     *
     * @param ticket     identificador del ticket
     * @param validation validación a aplicar antes de mover el archivo
     * @return ruta final donde quedó almacenado el archivo
     * @throws TempUploadTicketNotFoundException      si el ticket no existe o ha expirado
     * @throws TempStorageValidationException         si la validación falla
     * @throws TempUploadContentTypeMismatchException si el content type real no coincide con el esperado
     * @throws TempUploadFileNotFoundException        si el archivo temporal no existe en el storage
     */
    public final String confirm(String ticket, TempStorageValidation validation) throws IOException, TempUploadTicketNotFoundException, TempStorageValidationException {
        var uploadTicket = ticketManager.findByTicket(ticket);

        validation.validate(uploadTicket, storage);

        var tempPath = uploadTicket.getTempPath();
        var targetPath = uploadTicket.getTargetPath();
        var contentType = uploadTicket.getContentType();

        if (contentType != null) {
            internalValidateContentType(tempPath, targetPath, contentType);
        }

        internalConfirm(tempPath, targetPath);

        ticketManager.deleteTicket(uploadTicket);
        return targetPath;
    }

    /**
     * Mueve el archivo desde el directorio temporal al destino final.
     * Las subclases pueden sobreescribir este métod para añadir comportamiento
     * transaccional u otras estrategias (ej. copiar en lugar de mover).
     *
     * @param tempPath   ruta temporal del archivo
     * @param targetPath ruta destino final
     */
    protected void internalConfirm(String tempPath, String targetPath) throws IOException {
        try {
            storage.move(tempPath, targetPath);
        } catch (StorageObjectNotFoundException e) {
            throw new TempUploadFileNotFoundException(tempPath, e);
        }
    }

    /**
     * Valida que el content type real del archivo coincida con el esperado,
     * leyendo solo los primeros {@link #contentTypeDetectionBytes} bytes para detectar
     * la firma binaria (magic bytes) sin descargar el archivo completo.
     *
     * @param tempPath    ruta temporal del archivo
     * @param targetPath  ruta destin, usada para inferir el nombre en la detección
     * @param contentType content type esperado
     * @throws TempUploadContentTypeMismatchException si el content type detectado no coincide
     * @throws TempUploadFileNotFoundException        si el archivo temporal no existe
     */
    protected void internalValidateContentType(String tempPath, String targetPath, String contentType) throws IOException {
        var stored = storage.get(tempPath, ByteRange.first(contentTypeDetectionBytes))
            .orElseThrow(() -> new TempUploadFileNotFoundException(tempPath));

        try (InputStream content = stored.openContent()) {
            var detected = detectContentType(content, extractFilename(targetPath));
            if (!detected.equals(contentType)) {
                throw new TempUploadContentTypeMismatchException(contentType, detected);
            }
        }
    }

    /**
     * Elimina los tickets expirados y sus archivos temporales asociados del storage.
     * Los archivos que ya no existan en storage se ignoran silenciosamente.
     * El borrado de tickets se realiza en lote.
     */
    public void purgeExpired() throws IOException {
        var expired = ticketManager.findPurgeableBefore(Instant.now());
        for (var uploadTicket : expired) {
            try {
                storage.remove(uploadTicket.getTempPath());
            } catch (StorageObjectNotFoundException ignored) {
            }
        }
        ticketManager.deleteTickets(expired);
    }

    private TempUploadTicket newTempUploadTicket(StorageUploadRef ref, StorageUploadRef tempRef, String ticket, Duration urlDuration, Duration purgeDuration) {

        var now = Instant.now();
        var expiresAt = now.plus(urlDuration);
        var purgeAt = now.plus(purgeDuration);

        return new TempUploadTicket() {

            @Override
            public String getTicket() {
                return ticket;
            }

            @Override
            public Instant getCreatedAt() {
                return now;
            }

            @Override
            public String getTargetPath() {
                return ref.getPath();
            }

            @Override
            public String getTempPath() {
                return tempRef.getPath();
            }

            @Override
            public Instant getExpiresAt() {
                return expiresAt;
            }

            @Override
            public Instant getPurgeAt() {
                return purgeAt;
            }

            @Override
            public @Nullable String getContentType() {
                return ref.getContentType();
            }
        };
    }
}
