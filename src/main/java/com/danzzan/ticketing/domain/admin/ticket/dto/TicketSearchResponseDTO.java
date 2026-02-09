package com.danzzan.ticketing.domain.admin.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "학생 학번 기준 티켓 조회 응답")
public class TicketSearchResponseDTO {
    @Schema(description = "조회 대상 공연 ID", example = "2")
    private Long eventId;
    @Schema(description = "조회한 학생 학번", example = "32221902")
    private String studentId;
    @Schema(description = "티켓 검색 결과 목록")
    private List<TicketSearchItemDTO> results;
}
