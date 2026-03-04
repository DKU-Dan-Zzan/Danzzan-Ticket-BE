package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;

public interface AdmissionService {

    TicketRequestStatus admit(String eventId, String userId);
}
