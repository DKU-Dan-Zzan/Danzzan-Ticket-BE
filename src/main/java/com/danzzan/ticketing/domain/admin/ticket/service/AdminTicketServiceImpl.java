package com.danzzan.ticketing.domain.admin.ticket.service;

import com.danzzan.ticketing.domain.admin.auth.exception.AdminAuthenticationException;
import com.danzzan.ticketing.domain.admin.auth.exception.AdminForbiddenException;
import com.danzzan.ticketing.domain.admin.ticket.dto.IssueTicketResponseDTO;
import com.danzzan.ticketing.domain.admin.ticket.dto.TicketSearchItemDTO;
import com.danzzan.ticketing.domain.admin.ticket.dto.TicketSearchResponseDTO;
import com.danzzan.ticketing.domain.ticket.exception.TicketAlreadyIssuedException;
import com.danzzan.ticketing.domain.ticket.exception.TicketEventMismatchException;
import com.danzzan.ticketing.domain.ticket.exception.TicketNotFoundException;
import com.danzzan.ticketing.domain.ticket.model.entity.TicketStatus;
import com.danzzan.ticketing.domain.ticket.model.entity.UserTicket;
import com.danzzan.ticketing.domain.ticket.repository.UserTicketRepository;
import com.danzzan.ticketing.domain.user.model.UserInfo;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.model.entity.UserRole;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTicketServiceImpl implements AdminTicketService {

    private final UserTicketRepository userTicketRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public TicketSearchResponseDTO searchTicketByStudentId(Long eventId, String studentId) {
        UserTicket ticket = userTicketRepository.findByEventIdAndUser_StudentId(eventId, studentId)
                .orElseThrow(TicketNotFoundException::new);

        TicketSearchItemDTO item = toSearchItem(ticket);

        return TicketSearchResponseDTO.builder()
                .eventId(eventId)
                .studentId(studentId)
                .results(List.of(item))
                .build();
    }

    @Override
    @Transactional
    public IssueTicketResponseDTO issueTicket(Long eventId, Long ticketId, String note) {
        UserTicket ticket = userTicketRepository.findById(ticketId)
                .orElseThrow(TicketNotFoundException::new);

        if (!ticket.getEvent().getId().equals(eventId)) {
            throw new TicketEventMismatchException();
        }

        if (ticket.getStatus() == TicketStatus.ISSUED) {
            throw new TicketAlreadyIssuedException(
                    ticket.getId(),
                    ticket.getIssuedAt() != null ? ticket.getIssuedAt().toString() : null,
                    ticket.getIssuerAdmin() != null ? ticket.getIssuerAdmin().getName() : null
            );
        }

        User admin = resolveAdmin();
        ticket.issue(admin);

        return IssueTicketResponseDTO.builder()
                .ticketId(ticket.getId())
                .status(ticket.getStatus())
                .issuedAt(ticket.getIssuedAt() != null ? ticket.getIssuedAt().toString() : null)
                .issuerAdminId(ticket.getIssuerAdmin() != null ? ticket.getIssuerAdmin().getId() : null)
                .issuerAdminName(ticket.getIssuerAdmin() != null ? ticket.getIssuerAdmin().getName() : null)
                .build();
    }

    private TicketSearchItemDTO toSearchItem(UserTicket ticket) {
        return TicketSearchItemDTO.builder()
                .ticketId(ticket.getId())
                .studentId(ticket.getUser().getStudentId())
                .name(ticket.getUser().getName())
                .college(ticket.getUser().getCollege())
                .major(ticket.getUser().getMajor())
                .status(ticket.getStatus())
                .issuedAt(ticket.getIssuedAt() != null ? ticket.getIssuedAt().toString() : null)
                .issuerAdminName(ticket.getIssuerAdmin() != null ? ticket.getIssuerAdmin().getName() : null)
                .build();
    }

    private User resolveAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AdminAuthenticationException();
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            throw new AdminAuthenticationException();
        }

        Long userId = extractUserId(principal);
        if (userId == null) {
            throw new AdminAuthenticationException();
        }

        User admin = userRepository.findById(userId)
                .orElseThrow(AdminAuthenticationException::new);

        if (admin.getRole() != UserRole.ROLE_ADMIN) {
            throw new AdminForbiddenException();
        }

        return admin;
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof UserInfo userInfo) {
            return userInfo.getId();
        }
        if (principal instanceof User user) {
            return user.getId();
        }
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return userRepository.findByStudentId(value)
                        .map(User::getId)
                        .orElse(null);
            }
        }
        return null;
    }
}
