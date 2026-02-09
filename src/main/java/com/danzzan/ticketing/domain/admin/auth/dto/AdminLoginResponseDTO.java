package com.danzzan.ticketing.domain.admin.auth.dto;

import com.danzzan.ticketing.domain.user.model.entity.SystemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 로그인 응답")
public class AdminLoginResponseDTO {
    @Schema(description = "임시 access token", example = "admin-access-token-temp-123e4567-e89b-12d3-a456-426614174000")
    private String accessToken;
    @Schema(description = "관리자 정보")
    private AdminInfoDTO admin;
    @Schema(description = "시스템 타입", example = "DANSPOT")
    private SystemType system;
}
