package com.danzzan.ticketing.domain.ticket.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseTicketEventDto {
    private final String id;
    private final String title;
    private final String eventDate;
    private final String eventTime;
    private final String ticketOpenAt;
    private final String status;          // "upcoming" | "open" | "soldout"
    private final int remainingCount;
    private final int totalCount;
}
