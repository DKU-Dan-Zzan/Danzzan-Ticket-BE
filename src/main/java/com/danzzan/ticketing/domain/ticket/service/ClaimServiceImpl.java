package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private static final String USER_CLAIMED_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    @Override
    public ClaimResult claim(String eventId, String userId) {
        String userKey = TicketRedisKeys.userKey(eventId, userId);
        String stockKey = TicketRedisKeys.stockKey(eventId);
        String statusKey = TicketRedisKeys.statusKey(eventId, userId);

        Boolean firstClaim = redisTemplate.opsForValue().setIfAbsent(userKey, USER_CLAIMED_VALUE);
        if (!Boolean.TRUE.equals(firstClaim)) {
            setStatus(statusKey, TicketRequestStatus.ALREADY);
            return ClaimResult.already();
        }

        Long remaining = redisTemplate.opsForValue().decrement(stockKey);
        if (remaining == null) {
            redisTemplate.delete(userKey);
            setStatus(statusKey, TicketRequestStatus.SOLD_OUT);
            return ClaimResult.soldOut();
        }

        if (remaining < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.delete(userKey);
            setStatus(statusKey, TicketRequestStatus.SOLD_OUT);
            return ClaimResult.soldOut();
        }

        setStatus(statusKey, TicketRequestStatus.SUCCESS);
        return ClaimResult.success(remaining);
    }

    private void setStatus(String statusKey, TicketRequestStatus status) {
        redisTemplate.opsForValue().set(statusKey, status.name());
    }
}
