package com.danzzan.ticketing.domain.ticket.dto;

import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "티켓 요청(선착순) 응답")
public class TicketRequestResponseDTO {

    @Schema(description = "요청 결과 상태", example = "SUCCESS")
    private TicketRequestStatus status;

    @Schema(description = "남은 재고(선택)", example = "42", nullable = true)
    private Long remaining;
}
