package com.danzzan.ticketing.domain.admin.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 로그아웃 응답")
public class AdminLogoutResponseDTO {
    @Schema(description = "로그아웃 성공 여부", example = "true")
    private boolean ok;
}
