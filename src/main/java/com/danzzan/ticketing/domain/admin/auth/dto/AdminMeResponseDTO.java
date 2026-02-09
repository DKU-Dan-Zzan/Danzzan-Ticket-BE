package com.danzzan.ticketing.domain.admin.auth.dto;

import com.danzzan.ticketing.domain.user.model.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현재 로그인한 관리자 정보 응답")
public class AdminMeResponseDTO {
    @Schema(description = "관리자 ID", example = "5")
    private Long adminId;
    @Schema(description = "관리자 이름", example = "김관리")
    private String adminName;
    @Schema(description = "관리자 학번", example = "32221902")
    private String studentId;
    @Schema(description = "권한", example = "ROLE_ADMIN")
    private UserRole role;
}
