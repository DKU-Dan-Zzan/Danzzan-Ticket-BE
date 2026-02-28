package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.event.model.entity.FestivalEvent;
import com.danzzan.ticketing.domain.event.model.entity.TicketingStatus;
import com.danzzan.ticketing.domain.event.repository.FestivalEventRepository;
import com.danzzan.ticketing.domain.event.exception.EventNotFoundException;
import com.danzzan.ticketing.domain.ticket.dto.*;
import com.danzzan.ticketing.domain.ticket.exception.AlreadyReservedException;
import com.danzzan.ticketing.domain.ticket.exception.DoubleClickException;
import com.danzzan.ticketing.domain.ticket.exception.EventNotOpenException;
import com.danzzan.ticketing.domain.ticket.exception.EventSoldOutException;
import com.danzzan.ticketing.domain.ticket.model.entity.TicketStatus;
import com.danzzan.ticketing.domain.ticket.model.entity.UserTicket;
import com.danzzan.ticketing.domain.ticket.repository.TicketRedisRepository;
import com.danzzan.ticketing.domain.ticket.repository.UserTicketRepository;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import com.danzzan.ticketing.domain.user.exception.UserNotFoundException;
import com.danzzan.ticketing.global.exception.RedisUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TicketService {

    private final FestivalEventRepository eventRepository;
    private final UserTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketRedisRepository ticketRedisRepository;

    // 이벤트 목록 조회 (로그인 불필요)
    public ResponseTicketEventListDto getTicketingEvents() {
        List<FestivalEvent> events = eventRepository.findAll();

        List<ResponseTicketEventDto> items = events.stream()
                .map(this::toTicketEventDto)
                .collect(Collectors.toList());

        return new ResponseTicketEventListDto(items);
    }

    // 티켓 예매 (로그인 필요) — Redis Lua 기반
    @Transactional
    public ResponseReserveTicketDto reserveTicket(Long userId, Long eventId) {
        // 1. Redis Lua Script로 원자적 예매 시도
        long luaResult;
        try {
            luaResult = ticketRedisRepository.tryReserve(eventId, userId);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 연결 실패: eventId={}, userId={}", eventId, userId, e);
            throw new RedisUnavailableException();
        }

        // 2. Lua 결과 처리
        if (luaResult == -1) {
            throw new EventNotOpenException();
        }
        if (luaResult == -2) {
            throw new DoubleClickException();
        }
        if (luaResult == -3) {
            throw new EventSoldOutException();
        }

        // 3. Redis 차감 성공 → DB INSERT
        try {
            FestivalEvent event = eventRepository.findById(eventId)
                    .orElseThrow(EventNotFoundException::new);

            User user = userRepository.findById(userId)
                    .orElseThrow(UserNotFoundException::new);

            // 순번 계산
            long currentCount = ticketRepository.countByEventId(eventId);
            int order = (int) currentCount + 1;

            UserTicket ticket = UserTicket.builder()
                    .user(user)
                    .event(event)
                    .ticketingOrder(order)
                    .build();

            try {
                ticketRepository.save(ticket);
            } catch (DataIntegrityViolationException e) {
                // UNIQUE 제약 위반 → Redis 복원
                ticketRedisRepository.restoreOne(eventId);
                throw new AlreadyReservedException("이미 예매 처리가 완료되었습니다. 내 티켓에서 확인해주세요.");
            }

            // 4. newRemaining == 0 → 자동 매진 처리
            if (luaResult == 0) {
                event.changeStatus(TicketingStatus.CLOSED);
                eventRepository.save(event);
                ticketRedisRepository.closeEvent(eventId);
                log.info("이벤트 자동 매진 처리: eventId={}", eventId);
            }

            ResponseMyTicketDto ticketDto = toMyTicketDto(ticket, event);
            return new ResponseReserveTicketDto(order, ticketDto);

        } catch (AlreadyReservedException | EventNotFoundException | UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            // DB 실패 시 Redis 복원
            ticketRedisRepository.restoreOne(eventId);
            log.error("DB 저장 실패, Redis 복원: eventId={}, userId={}", eventId, userId, e);
            throw e;
        }
    }

    // 내 티켓 목록 조회 (로그인 필요)
    public ResponseMyTicketListDto getMyTickets(Long userId) {
        List<UserTicket> tickets = ticketRepository.findAllByUserIdOrderByTicketingAtDesc(userId);

        List<ResponseMyTicketDto> items = tickets.stream()
                .map(t -> toMyTicketDto(t, t.getEvent()))
                .collect(Collectors.toList());

        return new ResponseMyTicketListDto(items);
    }

    // 잔여석 조회 (폴링용) — Redis에서 직접 읽기
    public int getRemaining(Long eventId) {
        try {
            Integer remaining = ticketRedisRepository.getRemaining(eventId);
            if (remaining != null) {
                return remaining;
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 연결 실패 (잔여석 조회): eventId={}", eventId, e);
            throw new RedisUnavailableException();
        }

        // Redis에 키가 없으면 DB 폴백
        FestivalEvent event = eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);
        long ticketCount = ticketRepository.countByEventId(eventId);
        return Math.max(0, event.getTotalCapacity() - (int) ticketCount);
    }

    // ===== 변환 메서드 =====

    private ResponseTicketEventDto toTicketEventDto(FestivalEvent event) {
        int remaining;

        // OPEN 상태인 이벤트는 Redis에서 잔여석 읽기
        if (event.getTicketingStatus() == TicketingStatus.OPEN) {
            try {
                Integer redisRemaining = ticketRedisRepository.getRemaining(event.getId());
                if (redisRemaining != null) {
                    remaining = redisRemaining;
                } else {
                    long ticketCount = ticketRepository.countByEventId(event.getId());
                    remaining = Math.max(0, event.getTotalCapacity() - (int) ticketCount);
                }
            } catch (RedisConnectionFailureException e) {
                long ticketCount = ticketRepository.countByEventId(event.getId());
                remaining = Math.max(0, event.getTotalCapacity() - (int) ticketCount);
            }
        } else {
            long ticketCount = ticketRepository.countByEventId(event.getId());
            remaining = Math.max(0, event.getTotalCapacity() - (int) ticketCount);
        }

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
