package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;

public interface ClaimService {

    ClaimResult claim(String eventId, String userId);
}
