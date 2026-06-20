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
     * Crea una nueva instancia de {@link TempUploadTicket} sin persistirla.
     * Los campos se asignan externamente antes de llamar a {@link #saveTicket}.
     */
    TempUploadTicket newTicket();

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
     * Devuelve todos los tickets cuya fecha de expiración sea anterior al instante indicado.
     *
     * @param instant instante de referencia
     * @return lista de tickets expirados
     */
    List<? extends TempUploadTicket> findExpiredBefore(Instant instant);

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
