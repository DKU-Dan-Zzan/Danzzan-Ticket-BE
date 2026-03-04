package com.danzzan.ticketing.domain.ticket.service.model;

import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;

public record ClaimResult(
        TicketRequestStatus status,
        Long remaining
) {
    public ClaimResult {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (status == TicketRequestStatus.SUCCESS && remaining == null) {
            throw new IllegalArgumentException("remaining must not be null when status is SUCCESS");
        }

        if ((status == TicketRequestStatus.SOLD_OUT || status == TicketRequestStatus.ALREADY) && remaining != null) {
            throw new IllegalArgumentException("remaining must be null when status is SOLD_OUT or ALREADY");
        }
    }

    public static ClaimResult success(long remaining) {
        return new ClaimResult(TicketRequestStatus.SUCCESS, remaining);
    }

    public static ClaimResult soldOut() {
        return new ClaimResult(TicketRequestStatus.SOLD_OUT, null);
    }

    public static ClaimResult already() {
        return new ClaimResult(TicketRequestStatus.ALREADY, null);
    }
}
