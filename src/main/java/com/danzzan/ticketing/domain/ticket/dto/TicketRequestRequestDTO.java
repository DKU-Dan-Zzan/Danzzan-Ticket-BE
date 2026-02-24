package com.danzzan.ticketing.domain.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "티켓 요청(선착순) 요청")
public class TicketRequestRequestDTO {

    @NotBlank(message = "eventId는 필수입니다.")
    @Schema(description = "이벤트 ID", example = "festival-day1")
    private String eventId;

    @NotBlank(message = "userId는 필수입니다.")
    @Schema(description = "사용자 ID", example = "32221902")
    private String userId;
}
