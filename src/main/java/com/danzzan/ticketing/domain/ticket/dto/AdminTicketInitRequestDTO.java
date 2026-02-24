package com.danzzan.ticketing.domain.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 티켓팅 초기화 요청")
public class AdminTicketInitRequestDTO {

    @NotBlank(message = "eventId는 필수입니다.")
    @Schema(description = "이벤트 ID", example = "festival-day1")
    private String eventId;

    @NotNull(message = "stock은 필수입니다.")
    @Min(value = 0, message = "stock은 0 이상이어야 합니다.")
    @Schema(description = "초기 재고 수량", example = "5000")
    private Long stock;
}
