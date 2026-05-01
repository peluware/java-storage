package com.peluware.storage.temp;

public interface TempUploadTicketManager<T extends TempUploadTicket> {

    T newTicket();

    T findByTicket(String ticket) throws TempUploadTicketNotFoundException;

    void deleteTicket(T ticket);
}
