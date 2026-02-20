package com.danzzan.ticketing.domain.ticket.repository;

import com.danzzan.ticketing.domain.ticket.model.entity.TicketStatus;
import com.danzzan.ticketing.domain.ticket.model.entity.UserTicket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTicketRepository extends JpaRepository<UserTicket, Long> {
    Optional<UserTicket> findByEventIdAndUser_StudentId(Long eventId, String studentId);
    Optional<UserTicket> findByIdAndEventId(Long ticketId, Long eventId);
    long countByEventId(Long eventId);
    long countByEventIdAndStatus(Long eventId, TicketStatus status);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    List<UserTicket> findAllByUserIdOrderByTicketingAtDesc(Long userId);
}
