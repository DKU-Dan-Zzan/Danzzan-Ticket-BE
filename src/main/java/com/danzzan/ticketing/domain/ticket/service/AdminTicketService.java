package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.dto.IssueTicketResponseDTO;
import com.danzzan.ticketing.domain.ticket.dto.TicketSearchResponseDTO;

public interface AdminTicketService {
    TicketSearchResponseDTO searchTicketByStudentId(Long eventId, String studentId);
    IssueTicketResponseDTO issueTicket(Long eventId, Long ticketId, String note);
    IssueTicketResponseDTO cancelIssueTicket(Long eventId, Long ticketId);
}
