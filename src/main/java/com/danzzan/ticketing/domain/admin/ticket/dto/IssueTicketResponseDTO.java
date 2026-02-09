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
@Schema(description = "팔찌 지급 처리 응답")
public class IssueTicketResponseDTO {
    @Schema(description = "티켓 ID", example = "101")
    private Long ticketId;
    @Schema(description = "변경된 티켓 상태", example = "ISSUED")
    private TicketStatus status;
    @Schema(description = "팔찌 지급 시각 (ISO-8601)", example = "2026-05-13T16:12:03")
    private String issuedAt;
    @Schema(description = "지급 처리 관리자 ID", example = "5")
    private Long issuerAdminId;
    @Schema(description = "지급 처리 관리자 이름", example = "김관리")
    private String issuerAdminName;
}
