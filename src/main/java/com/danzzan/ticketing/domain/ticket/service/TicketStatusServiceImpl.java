package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketStatusServiceImpl implements TicketStatusService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public TicketRequestStatus getStatus(String eventId, String userId) {
        String statusKey = TicketRedisKeys.statusKey(eventId, userId);
        String statusValue = redisTemplate.opsForValue().get(statusKey);

        if (statusValue == null || statusValue.isBlank()) {
            return TicketRequestStatus.NONE;
        }

        try {
            return TicketRequestStatus.valueOf(statusValue);
        } catch (IllegalArgumentException ignored) {
            return TicketRequestStatus.NONE;
        }
    }
}
