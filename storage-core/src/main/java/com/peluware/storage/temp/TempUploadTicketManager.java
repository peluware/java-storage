package com.peluware.storage.temp;

import java.time.Instant;
import java.util.List;

public interface TempUploadTicketManager<T extends TempUploadTicket> {

    T newTicket();

    void saveTicket(T ticket);

    T findByTicket(String ticket) throws TempUploadTicketNotFoundException;

    List<T> findExpiredBefore(Instant instant);

    void deleteTicket(T ticket);

    void deleteTickets(List<T> tickets);
}
