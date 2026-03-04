package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;
import org.springframework.stereotype.Service;

@Service
public class ClaimServiceImpl implements ClaimService {

    @Override
    public ClaimResult claim(String eventId, String userId) {
        return ClaimResult.soldOut();
    }
}
