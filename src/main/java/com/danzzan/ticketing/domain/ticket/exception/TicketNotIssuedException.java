package com.danzzan.ticketing.domain.ticket.exception;

public class TicketNotIssuedException extends RuntimeException {
    public TicketNotIssuedException() {
        super("아직 팔찌가 지급되지 않은 티켓입니다.");
    }
}
