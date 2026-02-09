package com.danzzan.ticketing.domain.ticket.exception;

import lombok.Getter;

@Getter
public class TicketAlreadyIssuedException extends RuntimeException {
    private final Long ticketId;
    private final String issuedAt;
    private final String issuerAdminName;

    public TicketAlreadyIssuedException(Long ticketId, String issuedAt, String issuerAdminName) {
        super("이미 지급 완료된 티켓입니다.");
        this.ticketId = ticketId;
        this.issuedAt = issuedAt;
        this.issuerAdminName = issuerAdminName;
    }
}
