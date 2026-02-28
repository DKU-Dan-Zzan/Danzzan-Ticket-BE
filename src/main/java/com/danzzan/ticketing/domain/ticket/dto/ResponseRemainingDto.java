package com.danzzan.ticketing.domain.ticket.dto;

import lombok.Getter;

@Getter
public class ResponseRemainingDto {
    private final Long eventId;
    private final int remaining;

    public ResponseRemainingDto(Long eventId, int remaining) {
        this.eventId = eventId;
        this.remaining = remaining;
    }
}
