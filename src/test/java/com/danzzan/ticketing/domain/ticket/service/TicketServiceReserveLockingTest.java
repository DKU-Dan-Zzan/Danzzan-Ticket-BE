package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.event.model.entity.FestivalEvent;
import com.danzzan.ticketing.domain.event.model.entity.TicketingStatus;
import com.danzzan.ticketing.domain.event.repository.FestivalEventRepository;
import com.danzzan.ticketing.domain.ticket.dto.ResponseReserveTicketDto;
import com.danzzan.ticketing.domain.ticket.exception.AlreadyReservedException;
import com.danzzan.ticketing.domain.ticket.exception.EventSoldOutException;
import com.danzzan.ticketing.domain.ticket.model.entity.UserTicket;
import com.danzzan.ticketing.domain.ticket.repository.UserTicketRepository;
import com.danzzan.ticketing.domain.user.model.entity.AcademicStatus;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.model.entity.UserRole;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceReserveLockingTest {

    @Mock
    private FestivalEventRepository eventRepository;

    @Mock
    private UserTicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void reserveUsesPessimisticEventLockAndReturnsTicket() {
        FestivalEvent event = openEvent(2);
        User user = sampleUser();

        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(ticketRepository.existsByUserIdAndEventId(10L, 1L)).thenReturn(false);
        when(ticketRepository.countByEventId(1L)).thenReturn(1L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(ticketRepository.save(any(UserTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseReserveTicketDto response = ticketService.reserveTicket(10L, 1L);

        assertThat(response.getQueueNumber()).isEqualTo(2);
        assertThat(response.getTicket()).isNotNull();
        verify(eventRepository).findByIdForUpdate(1L);
        verify(eventRepository, never()).findById(any(Long.class));
    }

    @Test
    void reserveThrowsSoldOutWhenCapacityReachedInsideLockedSection() {
        FestivalEvent event = openEvent(1);

        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(ticketRepository.existsByUserIdAndEventId(10L, 1L)).thenReturn(false);
        when(ticketRepository.countByEventId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> ticketService.reserveTicket(10L, 1L))
                .isInstanceOf(EventSoldOutException.class);

        verify(userRepository, never()).findById(ArgumentMatchers.anyLong());
        verify(ticketRepository, never()).save(any(UserTicket.class));
    }

    @Test
    void reserveThrowsAlreadyReservedBeforeInsert() {
        FestivalEvent event = openEvent(3);

        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(ticketRepository.existsByUserIdAndEventId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> ticketService.reserveTicket(10L, 1L))
                .isInstanceOf(AlreadyReservedException.class);

        verify(ticketRepository, never()).countByEventId(1L);
        verify(ticketRepository, never()).save(any(UserTicket.class));
    }

    private FestivalEvent openEvent(int capacity) {
        return FestivalEvent.builder()
                .title("loadtest-event")
                .eventDate(LocalDate.of(2026, 5, 13))
                .ticketingStartTime(LocalDateTime.of(2026, 5, 13, 12, 0))
                .ticketingStatus(TicketingStatus.OPEN)
                .totalCapacity(capacity)
                .build();
    }

    private User sampleUser() {
        return User.builder()
                .studentId("loadtest-000001")
                .password("encoded")
                .name("Loadtest User 1")
                .college("SW융합대학")
                .major("소프트웨어학과")
                .academicStatus(AcademicStatus.ENROLLED)
                .role(UserRole.ROLE_USER)
                .build();
    }
}
