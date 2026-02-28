package com.danzzan.ticketing.domain.ticket.controller;

import com.danzzan.ticketing.domain.ticket.dto.ResponseMyTicketListDto;
import com.danzzan.ticketing.domain.ticket.dto.ResponseRemainingDto;
import com.danzzan.ticketing.domain.ticket.dto.ResponseReserveTicketDto;
import com.danzzan.ticketing.domain.ticket.dto.ResponseTicketEventListDto;
import com.danzzan.ticketing.domain.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "사용자 티켓팅", description = "공연 티켓 예매 및 조회 API")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/events")
    @Operation(summary = "이벤트 목록 조회", description = "티켓팅 가능한 공연 목록을 조회합니다. 로그인 불필요.")
    public ResponseEntity<ResponseTicketEventListDto> getTicketingEvents() {
        ResponseTicketEventListDto response = ticketService.getTicketingEvents();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/reserve")
    @Operation(summary = "티켓 예매", description = "선착순으로 공연 티켓을 예매합니다. 로그인 필요.")
    public ResponseEntity<ResponseReserveTicketDto> reserveTicket(
            @PathVariable Long eventId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ResponseReserveTicketDto response = ticketService.reserveTicket(userId, eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events/{eventId}/remaining")
    @Operation(summary = "잔여석 조회 (폴링용)", description = "Redis에서 실시간 잔여석을 조회합니다. 로그인 불필요.")
    public ResponseEntity<ResponseRemainingDto> getRemaining(@PathVariable Long eventId) {
        int remaining = ticketService.getRemaining(eventId);
        return ResponseEntity.ok(new ResponseRemainingDto(eventId, remaining));
    }

    @GetMapping("/me")
    @Operation(summary = "내 티켓 조회", description = "내가 예매한 티켓 목록을 조회합니다. 로그인 필요.")
    public ResponseEntity<ResponseMyTicketListDto> getMyTickets(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ResponseMyTicketListDto response = ticketService.getMyTickets(userId);
        return ResponseEntity.ok(response);
    }
}
