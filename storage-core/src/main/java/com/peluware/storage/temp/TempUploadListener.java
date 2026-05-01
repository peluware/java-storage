package com.peluware.storage.temp;

public interface TempUploadListener<T extends TempUploadTicket> {

    default void onTicketsGenerated(T ticketRef, TempUploadTickets tickets) {}

    default void onConfirmed(T ticketRef) {}
}
