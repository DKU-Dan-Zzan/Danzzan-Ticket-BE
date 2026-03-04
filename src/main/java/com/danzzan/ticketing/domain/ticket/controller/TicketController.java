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
import com.danzzan.ticketing.domain.ticket.service.TicketStatusService;
import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "사용자 티켓팅", description = "공연 티켓 예매 및 조회 API")
public class TicketController {

    private static final String LEGACY_SUNSET_DATE = "Tue, 30 Jun 2026 23:59:59 GMT";
    private static final String HEADER_DEPRECATION = "Deprecation";
    private static final String HEADER_SUNSET = "Sunset";

    private static final Set<TicketRequestStatus> CLAIM_TERMINAL_STATUSES = Set.of(
            TicketRequestStatus.SUCCESS,
            TicketRequestStatus.SOLD_OUT,
            TicketRequestStatus.ALREADY
    );

    private final TicketService ticketService;
    private final AdmissionService admissionService;
    private final ClaimService claimService;
    private final TicketStatusService ticketStatusService;

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

    @PostMapping("/{eventId}/queue/enter")
    @Operation(summary = "대기열 진입", description = "인증 사용자 기준으로 대기열에 진입하고 ADMITTED인 경우 즉시 claim을 수행합니다.")
    public ResponseEntity<TicketRequestResponseDTO> enterQueue(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        TicketRequestResponseDTO response = enterQueueAndClaim(String.valueOf(eventId), String.valueOf(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}/queue/status")
    @Operation(summary = "대기열 상태 조회", description = "인증 사용자 기준으로 대기열 상태를 조회합니다.")
    public ResponseEntity<TicketStatusResponseDTO> getQueueStatus(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        TicketStatusResponseDTO response = ticketStatus(String.valueOf(eventId), String.valueOf(userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request")
    @Operation(
            summary = "티켓 요청(v1, deprecated)",
            description = "레거시 호환용 엔드포인트입니다. 신규 클라이언트는 /tickets/{eventId}/queue/enter 사용을 권장합니다.",
            deprecated = true
    )
    public ResponseEntity<TicketRequestResponseDTO> requestTicket(
            @Valid @RequestBody TicketRequestRequestDTO request
    ) {
        TicketRequestResponseDTO response = enterQueueAndClaim(request.getEventId(), request.getUserId());
        return legacyResponse(response);
    }

    @GetMapping("/status")
    @Operation(
            summary = "티켓 상태 조회(v1, deprecated)",
            description = "레거시 호환용 엔드포인트입니다. 신규 클라이언트는 /tickets/{eventId}/queue/status 사용을 권장합니다.",
            deprecated = true
    )
    public ResponseEntity<TicketStatusResponseDTO> getTicketStatus(
            @Valid @ModelAttribute TicketStatusRequestDTO request
    ) {
        TicketStatusResponseDTO response = ticketStatus(request.getEventId(), request.getUserId());
        return legacyResponse(response);
    }

    private TicketRequestResponseDTO enterQueueAndClaim(String eventId, String userId) {
        TicketRequestStatus admissionStatus = admissionService.admit(eventId, userId);
        if (admissionStatus != TicketRequestStatus.ADMITTED) {
            return TicketRequestResponseDTO.builder()
                    .status(admissionStatus)
                    .remaining(null)
                    .build();
        }

        ClaimResult claimResult = claimService.claim(eventId, userId);
        if (!CLAIM_TERMINAL_STATUSES.contains(claimResult.status())) {
            throw new IllegalStateException("claim status must be one of SUCCESS, SOLD_OUT, ALREADY");
        }

        return TicketRequestResponseDTO.builder()
                .status(claimResult.status())
                .remaining(claimResult.remaining())
                .build();
    }

    private TicketStatusResponseDTO ticketStatus(String eventId, String userId) {
        TicketRequestStatus status = ticketStatusService.getStatus(eventId, userId);
        return TicketStatusResponseDTO.builder()
                .status(status)
                .build();
    }

    private <T> ResponseEntity<T> legacyResponse(T body) {
        return ResponseEntity.ok()
                .header(HEADER_DEPRECATION, "true")
                .header(HEADER_SUNSET, LEGACY_SUNSET_DATE)
                .body(body);
    }
}
