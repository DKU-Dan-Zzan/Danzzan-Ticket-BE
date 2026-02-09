package com.danzzan.ticketing.domain.admin.ticket.dto;

import com.danzzan.ticketing.domain.ticket.model.entity.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "티켓 검색 결과 항목")
public class TicketSearchItemDTO {
    @Schema(description = "티켓 ID (user_tickets.id)", example = "101")
    private Long ticketId;
    @Schema(description = "학생 학번", example = "32221902")
    private String studentId;
    @Schema(description = "학생 이름", example = "박주희")
    private String name;
    @Schema(description = "단과대학", example = "SW융합대학")
    private String college;
    @Schema(description = "학과", example = "소프트웨어학과")
    private String major;
    @Schema(description = "티켓 상태 (CONFIRMED / ISSUED)", example = "CONFIRMED")
    private TicketStatus status;
    @Schema(description = "팔찌 지급 시각 (ISO-8601)", example = "2026-05-13T14:20:30")
    private String issuedAt;
    @Schema(description = "지급 처리한 관리자 이름", example = "관리자A")
    private String issuerAdminName;
}
