package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.event.model.entity.FestivalEvent;
import com.danzzan.ticketing.domain.event.model.entity.TicketingStatus;
import com.danzzan.ticketing.domain.event.repository.FestivalEventRepository;
import com.danzzan.ticketing.domain.event.exception.EventNotFoundException;
import com.danzzan.ticketing.domain.ticket.dto.*;
import com.danzzan.ticketing.domain.ticket.exception.AlreadyReservedException;
import com.danzzan.ticketing.domain.ticket.exception.EventNotOpenException;
import com.danzzan.ticketing.domain.ticket.exception.EventSoldOutException;
import com.danzzan.ticketing.domain.ticket.model.entity.TicketStatus;
import com.danzzan.ticketing.domain.ticket.model.entity.UserTicket;
import com.danzzan.ticketing.domain.ticket.repository.UserTicketRepository;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import com.danzzan.ticketing.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TicketService {

    private final FestivalEventRepository eventRepository;
    private final UserTicketRepository ticketRepository;
    private final UserRepository userRepository;

    // 이벤트 목록 조회 (로그인 불필요)
    public ResponseTicketEventListDto getTicketingEvents() {
        List<FestivalEvent> events = eventRepository.findAll();

        List<ResponseTicketEventDto> items = events.stream()
                .map(this::toTicketEventDto)
                .collect(Collectors.toList());

        return new ResponseTicketEventListDto(items);
    }

    // 티켓 예매 (로그인 필요)
    @Transactional
    public ResponseReserveTicketDto reserveTicket(Long userId, Long eventId) {
        // 1. 이벤트 조회
        FestivalEvent event = eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);

        // 2. 오픈 전 체크 (FE에서도 막지만 BE 방어)
        if (event.getTicketingStatus() == TicketingStatus.READY) {
            throw new EventNotOpenException();
        }

        // 3. 마감 체크 (FE에서도 막지만 BE 방어)
        if (event.getTicketingStatus() == TicketingStatus.CLOSED) {
            throw new EventSoldOutException();
        }

        // 4. 중복 예매 체크
        if (ticketRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new AlreadyReservedException();
        }

        // 5. 잔여석 체크
        long currentCount = ticketRepository.countByEventId(eventId);
        if (currentCount >= event.getTotalCapacity()) {
            throw new EventSoldOutException();
        }

        // 6. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 7. 순번 계산 + 티켓 생성
        int order = (int) currentCount + 1;
        UserTicket ticket = UserTicket.builder()
                .user(user)
                .event(event)
                .ticketingOrder(order)
                .build();

        // 8. DB 저장 (동시 요청 시 unique 제약 위반 가능)
        try {
            ticketRepository.save(ticket);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyReservedException("이미 예매 처리가 완료되었습니다. 내 티켓에서 확인해주세요.");
        }

        // 9. 응답 생성
        ResponseMyTicketDto ticketDto = toMyTicketDto(ticket, event);
        return new ResponseReserveTicketDto(order, ticketDto);
    }

    // 내 티켓 목록 조회 (로그인 필요)
    public ResponseMyTicketListDto getMyTickets(Long userId) {
        List<UserTicket> tickets = ticketRepository.findAllByUserIdOrderByTicketingAtDesc(userId);

        List<ResponseMyTicketDto> items = tickets.stream()
                .map(t -> toMyTicketDto(t, t.getEvent()))
                .collect(Collectors.toList());

        return new ResponseMyTicketListDto(items);
    }

    // ===== 변환 메서드 =====

    private ResponseTicketEventDto toTicketEventDto(FestivalEvent event) {
        long ticketCount = ticketRepository.countByEventId(event.getId());
        int remaining = Math.max(0, event.getTotalCapacity() - (int) ticketCount);

        // BE status → FE status 변환
        String feStatus;
        switch (event.getTicketingStatus()) {
            case READY -> feStatus = "upcoming";
            case OPEN -> feStatus = remaining > 0 ? "open" : "soldout";
            case CLOSED -> feStatus = "soldout";
            default -> feStatus = "upcoming";
        }

        // 날짜 포맷팅
        LocalDate date = event.getEventDate();
        String[] dayOfWeekKor = {"", "월", "화", "수", "목", "금", "토", "일"};
        String formattedDate = String.format("%02d월 %02d일 (%s)",
                date.getMonthValue(), date.getDayOfMonth(),
                dayOfWeekKor[date.getDayOfWeek().getValue()]);

        String formattedTime = event.getTicketingStartTime()
                .format(DateTimeFormatter.ofPattern("HH:mm")) + " 예매 오픈";

        return ResponseTicketEventDto.builder()
                .id(String.valueOf(event.getId()))
                .title(event.getTitle())
                .eventDate(formattedDate)
                .eventTime(formattedTime)
                .ticketOpenAt(event.getTicketingStartTime().toString())
                .status(feStatus)
                .remainingCount(remaining)
                .totalCount(event.getTotalCapacity())
                .build();
    }

    private ResponseMyTicketDto toMyTicketDto(UserTicket ticket, FestivalEvent event) {
        // CONFIRMED → "issued" (팔찌 미수령), ISSUED → "used" (팔찌 수령완료)
        String feStatus = ticket.getStatus() == TicketStatus.CONFIRMED ? "issued" : "used";
        boolean wristbandIssued = ticket.getStatus() == TicketStatus.ISSUED;

        // 날짜 포맷팅
        LocalDate date = event.getEventDate();
        String[] dayOfWeekKor = {"", "월", "화", "수", "목", "금", "토", "일"};
        String formattedDate = String.format("%02d월 %02d일 (%s) 19:00",
                date.getMonthValue(), date.getDayOfMonth(),
                dayOfWeekKor[date.getDayOfWeek().getValue()]);

        // 몇 일차 계산 (첫 이벤트 기준)
        String eventName = event.getTitle();

        return ResponseMyTicketDto.builder()
                .id(String.valueOf(ticket.getId()))
                .status(feStatus)
                .eventName(eventName)
                .eventDate(formattedDate)
                .issuedAt(ticket.getTicketingAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .seat("단국존 순번 #" + ticket.getTicketingOrder())
                .queueNumber(ticket.getTicketingOrder())
                .wristbandIssued(wristbandIssued)
                .venue("단국존")
                .contact("축제 운영본부")
                .eventDescription(event.getTitle())
                .build();
    }
}
