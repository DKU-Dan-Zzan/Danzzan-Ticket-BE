package com.danzzan.ticketing.domain.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 티켓팅 초기화 응답")
public class AdminTicketInitResponseDTO {

    @Schema(description = "이벤트 ID", example = "festival-day1")
    private String eventId;

    @Schema(description = "초기 재고 수량", example = "2000")
    private Long stock;
}
