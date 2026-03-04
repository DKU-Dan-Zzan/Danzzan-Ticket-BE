package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ClaimServiceImpl claimService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsAlreadyWhenUserKeyAlreadyExists() {
        String eventId = "festival-day1";
        String userId = "32221902";
        String userKey = TicketRedisKeys.userKey(eventId, userId);
        String statusKey = TicketRedisKeys.statusKey(eventId, userId);
        String stockKey = TicketRedisKeys.stockKey(eventId);

        when(valueOperations.setIfAbsent(userKey, "1")).thenReturn(false);

        ClaimResult result = claimService.claim(eventId, userId);

        assertThat(result.status()).isEqualTo(TicketRequestStatus.ALREADY);
        assertThat(result.remaining()).isNull();
        verify(valueOperations, never()).decrement(stockKey);
        verify(valueOperations).set(statusKey, TicketRequestStatus.ALREADY.name());
    }

    @Test
    void returnsSoldOutWhenDecrementResultIsNegative() {
        String eventId = "festival-day1";
        String userId = "32221902";
        String userKey = TicketRedisKeys.userKey(eventId, userId);
        String statusKey = TicketRedisKeys.statusKey(eventId, userId);
        String stockKey = TicketRedisKeys.stockKey(eventId);

        when(valueOperations.setIfAbsent(userKey, "1")).thenReturn(true);
        when(valueOperations.decrement(stockKey)).thenReturn(-1L);

        ClaimResult result = claimService.claim(eventId, userId);

        assertThat(result.status()).isEqualTo(TicketRequestStatus.SOLD_OUT);
        assertThat(result.remaining()).isNull();
        verify(valueOperations).increment(stockKey);
        verify(redisTemplate).delete(userKey);
        verify(valueOperations).set(statusKey, TicketRequestStatus.SOLD_OUT.name());
    }

    @Test
    void returnsSuccessWithRemainingWhenClaimSucceeds() {
        String eventId = "festival-day1";
        String userId = "32221902";
        String userKey = TicketRedisKeys.userKey(eventId, userId);
        String statusKey = TicketRedisKeys.statusKey(eventId, userId);
        String stockKey = TicketRedisKeys.stockKey(eventId);

        when(valueOperations.setIfAbsent(userKey, "1")).thenReturn(true);
        when(valueOperations.decrement(stockKey)).thenReturn(42L);

        ClaimResult result = claimService.claim(eventId, userId);

        assertThat(result.status()).isEqualTo(TicketRequestStatus.SUCCESS);
        assertThat(result.remaining()).isEqualTo(42L);
        verify(valueOperations).set(statusKey, TicketRequestStatus.SUCCESS.name());
    }
}
