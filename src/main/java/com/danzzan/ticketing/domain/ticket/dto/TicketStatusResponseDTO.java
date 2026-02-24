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
@Schema(description = "티켓 상태 조회 응답")
public class TicketStatusResponseDTO {

    @Schema(description = "현재 상태", example = "WAITING")
    private TicketRequestStatus status;
}
