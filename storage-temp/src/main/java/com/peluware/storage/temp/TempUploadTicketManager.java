package com.peluware.storage.temp;

import java.time.Instant;
import java.util.List;

/**
 * Contrato para la persistencia y recuperación de {@link TempUploadTicket}.
 * <p>
 * Las implementaciones son responsables de almacenar los tickets en el medio
 * de persistencia elegido (JPA, Redis, en memoria, etc.).
 */
public interface TempUploadTicketManager {

    /**
     * Persiste el ticket en el medio de almacenamiento.
     *
     * @param ticket ticket a guardar
     */
    void saveTicket(TempUploadTicket ticket);

    /**
     * Recupera un ticket por su identificador.
     *
     * @param ticket identificador del ticket
     * @return el ticket encontrado
     * @throws TempUploadTicketNotFoundException si no existe un ticket con ese identificador
     */
    TempUploadTicket findByTicket(String ticket) throws TempUploadTicketNotFoundException;

    /**
     * Devuelve todos los tickets cuyo {@link TempUploadTicket#getPurgeAt()} sea anterior al instante indicado.
     * Estos tickets y sus archivos temporales son candidatos a ser eliminados por {@link TempStorage#purgeExpired}.
     *
     * @param instant instante de referencia
     * @return lista de tickets purgables
     */
    List<? extends TempUploadTicket> findPurgeableBefore(Instant instant);

    /**
     * Elimina un ticket del medio de almacenamiento.
     *
     * @param ticket ticket a eliminar
     */
    void deleteTicket(TempUploadTicket ticket);

    /**
     * Elimina múltiples tickets en una sola operación.
     * Las implementaciones deben optimizar esto en una única consulta de borrado en lote.
     *
     * @param tickets lista de tickets a eliminar
     */
    void deleteTickets(List<? extends TempUploadTicket> tickets);
}
