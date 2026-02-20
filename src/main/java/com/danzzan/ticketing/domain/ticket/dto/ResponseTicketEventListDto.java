package com.danzzan.ticketing.domain.ticket.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ResponseTicketEventListDto {
    private final List<ResponseTicketEventDto> items;

    public ResponseTicketEventListDto(List<ResponseTicketEventDto> items) {
        this.items = items;
    }
}
