package com.danzzan.ticketing.domain.ticket.redis;

public enum TicketRequestStatus {
    NONE,
    WAITING,
    ADMITTED,
    SUCCESS,
    SOLD_OUT,
    ALREADY
}
