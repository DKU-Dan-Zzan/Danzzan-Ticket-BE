package com.danzzan.ticketing.domain.ticket.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ResponseMyTicketListDto {
    private final List<ResponseMyTicketDto> items;

    public ResponseMyTicketListDto(List<ResponseMyTicketDto> items) {
        this.items = items;
    }
}
