package com.danzzan.ticketing.domain.admin.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공연별 팔찌 지급 통계 응답")
public class EventStatsResponseDTO {
    @Schema(description = "공연 ID", example = "2")
    private Long eventId;
    @Schema(description = "공연 제목", example = "5월 13일 공연 팔찌 배부")
    private String title;
    @Schema(description = "공연 날짜 (YYYY-MM-DD)", example = "2026-05-13")
    private String eventDate;
    @Schema(description = "정원", example = "5000")
    private int totalCapacity;
    @Schema(description = "해당 공연에 대해 발급된 티켓 수", example = "4820")
    private long totalTickets;
    @Schema(description = "status=CONFIRMED 티켓 수", example = "310")
    private long ticketsConfirmed;
    @Schema(description = "status=ISSUED 티켓 수", example = "4510")
    private long ticketsIssued;
    @Schema(description = "지급 완료 비율(%)", example = "93.57")
    private double issueRate;
    @Schema(description = "남은 수용 가능 인원", example = "180")
    private int remainingCapacity;
}
