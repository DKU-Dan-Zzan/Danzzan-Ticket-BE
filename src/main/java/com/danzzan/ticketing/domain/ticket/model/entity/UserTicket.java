package com.danzzan.ticketing.domain.ticket.model.entity;

import com.danzzan.ticketing.domain.event.model.entity.FestivalEvent;
import com.danzzan.ticketing.domain.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_tickets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_event", columnNames = {"user_id", "event_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private FestivalEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "ticketing_order", nullable = false)
    private Integer ticketingOrder;

    @Column(name = "ticketing_at", nullable = false)
    private LocalDateTime ticketingAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_admin_id")
    private User issuerAdmin;

    @Builder
    public UserTicket(User user, FestivalEvent event, Integer ticketingOrder) {
        this.user = user;
        this.event = event;
        this.status = TicketStatus.CONFIRMED;
        this.ticketingOrder = ticketingOrder;
        this.ticketingAt = LocalDateTime.now();
    }

    // 팔찌 수령 처리
    // 티켓 상태를 ISSUED로 변경하고 수령 시간과 발급 관리자 기록
    public void issue(User admin) {
        this.status = TicketStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
        this.issuerAdmin = admin;
    }

    // 팔찌 지급 취소
    // 티켓 상태를 CONFIRMED로 되돌리고 수령 정보 초기화
    public void cancelIssue() {
        this.status = TicketStatus.CONFIRMED;
        this.issuedAt = null;
        this.issuerAdmin = null;
    }
}
