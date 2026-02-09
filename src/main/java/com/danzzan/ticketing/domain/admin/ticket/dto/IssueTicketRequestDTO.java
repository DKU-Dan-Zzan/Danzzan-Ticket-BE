package com.danzzan.ticketing.domain.admin.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팔찌 지급 처리 요청")
public class IssueTicketRequestDTO {
    @Schema(description = "지급 처리 메모(선택). 현재 서버 저장 없이 사용", example = "현장 확인 완료")
    private String note;
}
