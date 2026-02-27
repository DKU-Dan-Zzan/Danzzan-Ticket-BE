package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.dto.AdminTicketInitResponseDTO;
import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketInitServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Cursor<String> userCursor;

    @Mock
    private Cursor<String> statusCursor;

    @InjectMocks
    private TicketInitServiceImpl ticketInitService;

    @Test
    void initStockWritesStockKeyAndCleansClaimKeys() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(userCursor, statusCursor);
        when(userCursor.hasNext()).thenReturn(true, true, false);
        when(userCursor.next()).thenReturn(
                TicketRedisKeys.userKey("festival-day1", "u1"),
                TicketRedisKeys.userKey("festival-day1", "u2")
        );
        when(statusCursor.hasNext()).thenReturn(true, false);
        when(statusCursor.next()).thenReturn(TicketRedisKeys.statusKey("festival-day1", "u1"));

        AdminTicketInitResponseDTO response = ticketInitService.initStock("festival-day1", 5000L);

        verify(redisTemplate, times(2)).unlink(anyCollection());
        verify(userCursor).close();
        verify(statusCursor).close();
        verify(valueOperations).set(TicketRedisKeys.stockKey("festival-day1"), "5000");
        assertThat(response.getEventId()).isEqualTo("festival-day1");
        assertThat(response.getStock()).isEqualTo(5000L);
    }

    @Test
    void initStockDoesNotCallUnlinkWhenNoClaimKeysExist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(userCursor, statusCursor);
        when(userCursor.hasNext()).thenReturn(false);
        when(statusCursor.hasNext()).thenReturn(false);

        ticketInitService.initStock("festival-day1", 5000L);

        verify(redisTemplate, never()).unlink(anyCollection());
        verify(userCursor).close();
        verify(statusCursor).close();
        verify(valueOperations).set(TicketRedisKeys.stockKey("festival-day1"), "5000");
    }
}
