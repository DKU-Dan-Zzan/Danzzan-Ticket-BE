package com.danzzan.ticketing.domain.ticket.exception;

public class TicketEventMismatchException extends RuntimeException {
    public TicketEventMismatchException() {
        super("ticketId와 eventId가 일치하지 않습니다.");
    }
}
