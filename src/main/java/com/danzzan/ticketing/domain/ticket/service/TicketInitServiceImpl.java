package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.dto.AdminTicketInitResponseDTO;
import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketInitServiceImpl implements TicketInitService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public AdminTicketInitResponseDTO initStock(String eventId, Long stock) {
        String stockKey = TicketRedisKeys.stockKey(eventId);
        redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));

        return AdminTicketInitResponseDTO.builder()
                .eventId(eventId)
                .stock(stock)
                .build();
    }
}
