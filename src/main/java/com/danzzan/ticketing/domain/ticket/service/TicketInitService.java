package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.dto.AdminTicketInitResponseDTO;

public interface TicketInitService {

    AdminTicketInitResponseDTO initStock(String eventId, Long stock);
}
