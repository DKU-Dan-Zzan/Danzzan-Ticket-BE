package com.danzzan.ticketing.domain.event.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "festival_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "ticketing_start_time", nullable = false)
    private LocalDateTime ticketingStartTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticketing_status", nullable = false)
    private TicketingStatus ticketingStatus;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Builder
    public FestivalEvent(String title, LocalDate eventDate, LocalDateTime ticketingStartTime,
                         TicketingStatus ticketingStatus, Integer totalCapacity) {
        this.title = title;
        this.eventDate = eventDate;
        this.ticketingStartTime = ticketingStartTime;
        this.ticketingStatus = ticketingStatus != null ? ticketingStatus : TicketingStatus.READY;
        this.totalCapacity = totalCapacity;
    }

    public void changeStatus(TicketingStatus status) {
        this.ticketingStatus = status;
    }

    public void updateCapacity(Integer capacity) {
        this.totalCapacity = capacity;
    }
}
