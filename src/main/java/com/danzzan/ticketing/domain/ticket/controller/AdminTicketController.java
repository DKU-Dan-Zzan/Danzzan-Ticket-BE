package com.danzzan.ticketing.domain.ticket.controller;

import com.danzzan.ticketing.global.model.ApiResponse;
import com.danzzan.ticketing.domain.ticket.dto.IssueTicketRequestDTO;
import com.danzzan.ticketing.domain.ticket.dto.IssueTicketResponseDTO;
import com.danzzan.ticketing.domain.ticket.dto.TicketSearchResponseDTO;
import com.danzzan.ticketing.domain.ticket.service.AdminTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "관리자 티켓", description = "관리자 티켓 조회/발급 API")
public class AdminTicketController {

    private final AdminTicketService adminTicketService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/events/{eventId}/tickets/search")
    @Operation(
            summary = "학생 학번 기준 티켓 조회",
            description = "특정 공연에서 학생 학번으로 티켓 정보를 조회"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "티켓 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "eventId": 2,
                                                "studentId": "32221902",
                                                "results": [
                                                  {
                                                    "ticketId": 101,
                                                    "studentId": "32221902",
                                                    "name": "김주희",
                                                    "college": "공과대학",
                                                    "major": "컴퓨터공학과",
                                                    "status": "CONFIRMED",
                                                    "issuedAt": null,
                                                    "issuerAdminName": null
                                                  }
                                                ]
                                              },
                                              "error": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "studentId 누락 또는 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 공연에서 해당 학번의 티켓이 존재하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ApiResponse<TicketSearchResponseDTO> searchTickets(
            @NotNull @PathVariable Long eventId,
            @NotBlank @RequestParam String studentId
    ) {
        return ApiResponse.success(adminTicketService.searchTicketByStudentId(eventId, studentId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/events/{eventId}/tickets/{ticketId}/issue")
    @Operation(
            summary = "팔찌 지급 처리",
            description = "티켓 상태를 ISSUED로 변경하고 지급 시각 및 지급 관리자 기록"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "팔찌 지급 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "ticketId": 101,
                                                "status": "ISSUED",
                                                "issuedAt": "2026-05-13T16:12:03",
                                                "issuerAdminId": 5,
                                                "issuerAdminName": "김관리"
                                              },
                                              "error": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 지급 완료된 티켓",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "already_issued",
                                    value = """
                                            {
                                              "success": false,
                                              "data": null,
                                              "error": {
                                                "error": "ALREADY_ISSUED",
                                                "message": "이미 지급 완료된 티켓입니다.",
                                                "ticketId": 101,
                                                "issuedAt": "2026-05-13T15:01:11",
                                                "issuerAdminName": "김관리"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "티켓 또는 공연이 존재하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ApiResponse<IssueTicketResponseDTO> issueTicket(
            @NotNull @PathVariable Long eventId,
            @NotNull @PathVariable Long ticketId,
            @Valid @RequestBody(required = false) IssueTicketRequestDTO request
    ) {
        String note = request != null ? request.getNote() : null;
        return ApiResponse.success(adminTicketService.issueTicket(eventId, ticketId, note));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/events/{eventId}/tickets/{ticketId}/cancel")
    @Operation(
            summary = "팔찌 지급 취소",
            description = "티켓 상태를 CONFIRMED로 되돌리고 지급 정보 초기화"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<IssueTicketResponseDTO> cancelIssueTicket(
            @NotNull @PathVariable Long eventId,
            @NotNull @PathVariable Long ticketId
    ) {
        return ApiResponse.success(adminTicketService.cancelIssueTicket(eventId, ticketId));
    }
}
