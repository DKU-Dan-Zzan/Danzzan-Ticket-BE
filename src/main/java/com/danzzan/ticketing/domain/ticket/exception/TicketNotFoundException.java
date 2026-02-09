package com.danzzan.ticketing.domain.ticket.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException() {
        super("해당 티켓을 찾을 수 없습니다.");
    }
}
