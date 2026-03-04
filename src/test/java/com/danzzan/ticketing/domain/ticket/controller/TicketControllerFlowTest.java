package com.danzzan.ticketing.domain.ticket.controller;

import com.danzzan.ticketing.domain.ticket.dto.TicketRequestRequestDTO;
import com.danzzan.ticketing.domain.ticket.dto.TicketRequestResponseDTO;
import com.danzzan.ticketing.domain.ticket.dto.TicketStatusRequestDTO;
import com.danzzan.ticketing.domain.ticket.dto.TicketStatusResponseDTO;
import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import com.danzzan.ticketing.domain.ticket.service.AdmissionService;
import com.danzzan.ticketing.domain.ticket.service.ClaimService;
import com.danzzan.ticketing.domain.ticket.service.TicketService;
import com.danzzan.ticketing.domain.ticket.service.TicketStatusService;
import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerFlowTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private AdmissionService admissionService;

    @Mock
    private ClaimService claimService;

    @Mock
    private TicketStatusService ticketStatusService;

    @InjectMocks
    private TicketController ticketController;

    @Test
    void doesNotCallClaimWhenAdmissionIsNotAdmitted() {
        TicketRequestRequestDTO request = TicketRequestRequestDTO.builder()
                .eventId("festival-day1")
                .userId("32221902")
                .build();

        when(admissionService.admit("festival-day1", "32221902"))
                .thenReturn(TicketRequestStatus.WAITING);

        ResponseEntity<TicketRequestResponseDTO> response = ticketController.requestTicket(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TicketRequestStatus.WAITING);
        assertThat(response.getBody().getRemaining()).isNull();
        verify(claimService, never()).claim("festival-day1", "32221902");
    }

    @Test
    void callsClaimWhenAdmissionIsAdmitted() {
        TicketRequestRequestDTO request = TicketRequestRequestDTO.builder()
                .eventId("festival-day1")
                .userId("32221902")
                .build();

        when(admissionService.admit("festival-day1", "32221902"))
                .thenReturn(TicketRequestStatus.ADMITTED);
        when(claimService.claim("festival-day1", "32221902"))
                .thenReturn(ClaimResult.success(10L));

        ResponseEntity<TicketRequestResponseDTO> response = ticketController.requestTicket(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TicketRequestStatus.SUCCESS);
        assertThat(response.getBody().getRemaining()).isEqualTo(10L);
        verify(claimService).claim("festival-day1", "32221902");
    }

    @Test
    void statusEndpointReturnsNoneWhenServiceReturnsNone() {
        TicketStatusRequestDTO request = TicketStatusRequestDTO.builder()
                .eventId("festival-day1")
                .userId("32221902")
                .build();
        when(ticketStatusService.getStatus("festival-day1", "32221902"))
                .thenReturn(TicketRequestStatus.NONE);

        ResponseEntity<TicketStatusResponseDTO> response = ticketController.getTicketStatus(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TicketRequestStatus.NONE);
    }
}
