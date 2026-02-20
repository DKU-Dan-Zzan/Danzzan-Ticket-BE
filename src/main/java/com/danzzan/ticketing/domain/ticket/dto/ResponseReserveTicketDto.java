package com.danzzan.ticketing.domain.ticket.dto;

import lombok.Getter;

@Getter
public class ResponseReserveTicketDto {
    private final int queueNumber;
    private final ResponseMyTicketDto ticket;

    public ResponseReserveTicketDto(int queueNumber, ResponseMyTicketDto ticket) {
        this.queueNumber = queueNumber;
        this.ticket = ticket;
    }
}
