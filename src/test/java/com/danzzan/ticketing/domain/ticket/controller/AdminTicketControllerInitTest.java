package com.danzzan.ticketing.domain.ticket.controller;

import com.danzzan.ticketing.domain.ticket.dto.AdminTicketInitRequestDTO;
import com.danzzan.ticketing.domain.ticket.dto.AdminTicketInitResponseDTO;
import com.danzzan.ticketing.domain.ticket.service.AdminTicketService;
import com.danzzan.ticketing.domain.ticket.service.TicketInitService;
import com.danzzan.ticketing.global.model.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTicketControllerInitTest {

    @Mock
    private AdminTicketService adminTicketService;

    @Mock
    private TicketInitService ticketInitService;

    @InjectMocks
    private AdminTicketController adminTicketController;

    @Test
    void initTicketStockReturnsSuccessResponse() {
        AdminTicketInitRequestDTO request = AdminTicketInitRequestDTO.builder()
                .eventId("festival-day1")
                .stock(5000L)
                .build();

        AdminTicketInitResponseDTO initResponse = AdminTicketInitResponseDTO.builder()
                .eventId("festival-day1")
                .stock(5000L)
                .build();
        when(ticketInitService.initStock("festival-day1", 5000L)).thenReturn(initResponse);

        ResponseEntity<ApiResponse<AdminTicketInitResponseDTO>> response = adminTicketController.initTicketStock(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getEventId()).isEqualTo("festival-day1");
        assertThat(response.getBody().getData().getStock()).isEqualTo(5000L);
        verify(ticketInitService).initStock("festival-day1", 5000L);
    }
}
