package com.peluware.storage.temp;

public interface TempUploadListener<T extends TempUploadTicket> {

    default void onTicketsGenerated(T uploadTicket, TempUploadTickets tickets) {}

    default void onConfirmed(T uploadTicket) {}
}
