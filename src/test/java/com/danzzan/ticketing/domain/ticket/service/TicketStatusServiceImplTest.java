package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketStatusServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TicketStatusServiceImpl ticketStatusService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsNoneWhenStatusKeyDoesNotExist() {
        String statusKey = TicketRedisKeys.statusKey("festival-day1", "32221902");
        when(valueOperations.get(statusKey)).thenReturn(null);

        TicketRequestStatus status = ticketStatusService.getStatus("festival-day1", "32221902");

        assertThat(status).isEqualTo(TicketRequestStatus.NONE);
    }

    @Test
    void returnsEnumStatusWhenStoredValueIsValid() {
        String statusKey = TicketRedisKeys.statusKey("festival-day1", "32221902");
        when(valueOperations.get(statusKey)).thenReturn("SUCCESS");

        TicketRequestStatus status = ticketStatusService.getStatus("festival-day1", "32221902");

        assertThat(status).isEqualTo(TicketRequestStatus.SUCCESS);
    }

    @Test
    void returnsNoneWhenStoredValueIsInvalid() {
        String statusKey = TicketRedisKeys.statusKey("festival-day1", "32221902");
        when(valueOperations.get(statusKey)).thenReturn("INVALID_STATUS");

        TicketRequestStatus status = ticketStatusService.getStatus("festival-day1", "32221902");

        assertThat(status).isEqualTo(TicketRequestStatus.NONE);
    }
}
