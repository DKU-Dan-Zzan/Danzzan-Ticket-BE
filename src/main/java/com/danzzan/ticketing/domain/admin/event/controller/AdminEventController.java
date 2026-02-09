package com.danzzan.ticketing.domain.admin.event.controller;

import com.danzzan.ticketing.global.model.ApiResponse;
import com.danzzan.ticketing.domain.admin.event.dto.EventListResponseDTO;
import com.danzzan.ticketing.domain.admin.event.dto.EventStatsResponseDTO;
import com.danzzan.ticketing.domain.admin.event.service.AdminEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
@Tag(name = "관리자 공연", description = "관리자용 공연/운영 관련 API")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/events")
    @Operation(
            summary = "팔찌배부대상 공연일 기준으로 목록 조회",
            description = "JWT 인증이 필요한 관리자 전용 목록 조회 API"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "events": [
                                                  {
                                                    "eventId": 2,
                                                    "title": "5월 13일 공연 팔찌 배부",
                                                    "dayLabel": "DAY 2",
                                                    "eventDate": "2026-05-13",
                                                    "ticketingStatus": "OPEN",
                                                    "totalCapacity": 5000
                                                  },
                                                  {
                                                    "eventId": 3,
                                                    "title": "5월 14일 공연 팔찌 배부",
                                                    "dayLabel": "DAY 3",
                                                    "eventDate": "2026-05-14",
                                                    "ticketingStatus": "READY",
                                                    "totalCapacity": 5000
                                                  }
                                                ]
                                              },
                                              "error": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiResponse<EventListResponseDTO> listEvents() {
        return ApiResponse.success(adminEventService.listEvents());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/events/{eventId}/stats")
    public ApiResponse<EventStatsResponseDTO> getEventStats(@NotNull @PathVariable Long eventId) {
        return ApiResponse.success(adminEventService.getEventStats(eventId));
    }
}
