package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;

public interface TicketStatusService {

    TicketRequestStatus getStatus(String eventId, String userId);
}
