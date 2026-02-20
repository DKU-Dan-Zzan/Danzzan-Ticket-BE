package com.danzzan.ticketing.domain.ticket.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseMyTicketDto {
    private final String id;
    private final String status;          // "issued" (미수령) | "used" (수령완료)
    private final String eventName;
    private final String eventDate;
    private final String issuedAt;
    private final String seat;
    private final int queueNumber;
    private final boolean wristbandIssued;
    private final String venue;
    private final String contact;
    private final String eventDescription;
}
