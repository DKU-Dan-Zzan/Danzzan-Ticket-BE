package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdmissionServiceImplTest {

    private final AdmissionService admissionService = new AdmissionServiceImpl();

    @Test
    void admitAlwaysReturnsAdmitted() {
        TicketRequestStatus status = admissionService.admit("festival-day1", "32221902");

        assertThat(status).isEqualTo(TicketRequestStatus.ADMITTED);
    }
}
