package com.peluware.storage.temp;

import java.util.List;

public interface TempUploadListener<T extends TempUploadTicket> {

    default void onTicketsGenerated(T uploadTicket, TempUploadTickets tickets) {}

    default void onConfirmed(T uploadTicket) {}

    default void onExpired(List<T> uploadTickets) {}
}
