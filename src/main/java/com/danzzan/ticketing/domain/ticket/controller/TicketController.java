package com.danzzan.ticketing.domain.ticket.controller;

import com.danzzan.ticketing.domain.ticket.dto.ResponseMyTicketListDto;
import com.danzzan.ticketing.domain.ticket.dto.ResponseReserveTicketDto;
import com.danzzan.ticketing.domain.ticket.dto.ResponseTicketEventListDto;
import com.danzzan.ticketing.domain.ticket.dto.TicketRequestRequestDTO;
import com.danzzan.ticketing.domain.ticket.dto.TicketRequestResponseDTO;
import com.danzzan.ticketing.domain.ticket.dto.TicketStatusRequestDTO;
import com.danzzan.ticketing.domain.ticket.dto.TicketStatusResponseDTO;
import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import com.danzzan.ticketing.domain.ticket.service.AdmissionService;
import com.danzzan.ticketing.domain.ticket.service.ClaimService;
import com.danzzan.ticketing.domain.ticket.service.TicketService;
import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "사용자 티켓팅", description = "공연 티켓 예매 및 조회 API")
public class TicketController {

    private final TicketService ticketService;
    private final AdmissionService admissionService;
    private final ClaimService claimService;

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

    @GetMapping("/me")
    @Operation(summary = "내 티켓 조회", description = "내가 예매한 티켓 목록을 조회합니다. 로그인 필요.")
    public ResponseEntity<ResponseMyTicketListDto> getMyTickets(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ResponseMyTicketListDto response = ticketService.getMyTickets(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request")
    @Operation(summary = "티켓 요청(계약)", description = "선착순 티켓 요청 계약 엔드포인트. 실제 로직은 추후 구현")
    public ResponseEntity<TicketRequestResponseDTO> requestTicket(
            @Valid @RequestBody TicketRequestRequestDTO request
    ) {
        TicketRequestStatus admissionStatus = admissionService.admit(request.getEventId(), request.getUserId());
        if (admissionStatus != TicketRequestStatus.ADMITTED) {
            return ResponseEntity.ok(TicketRequestResponseDTO.builder()
                    .status(admissionStatus)
                    .remaining(null)
                    .build());
        }

        ClaimResult claimResult = claimService.claim(request.getEventId(), request.getUserId());
        return ResponseEntity.ok(TicketRequestResponseDTO.builder()
                .status(claimResult.status())
                .remaining(claimResult.remaining())
                .build());
    }

    @GetMapping("/status")
    @Operation(summary = "티켓 상태 조회(계약)", description = "폴링 대비 상태 조회 계약 엔드포인트. 실제 로직은 추후 구현")
    public ResponseEntity<TicketStatusResponseDTO> getTicketStatus(
            @Valid @ModelAttribute TicketStatusRequestDTO request
    ) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "TODO: ticket status logic");
    }
}
