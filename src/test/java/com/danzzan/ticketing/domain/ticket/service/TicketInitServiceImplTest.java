package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.dto.AdminTicketInitResponseDTO;
import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketInitServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TicketInitServiceImpl ticketInitService;

    @Test
    void initStockWritesStockKeyAndReturnsResponse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AdminTicketInitResponseDTO response = ticketInitService.initStock("festival-day1", 5000L);

        verify(valueOperations).set(TicketRedisKeys.stockKey("festival-day1"), "5000");
        assertThat(response.getEventId()).isEqualTo("festival-day1");
        assertThat(response.getStock()).isEqualTo(5000L);
    }
}
