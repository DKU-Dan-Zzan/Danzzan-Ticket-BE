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
@Schema(description = "관리자 정보")
public class AdminInfoDTO {
    @Schema(description = "관리자 ID", example = "5")
    private Long id;
    @Schema(description = "관리자 이름", example = "김관리")
    private String name;
    @Schema(description = "관리자 학번", example = "32221902")
    private String studentId;
    @Schema(description = "권한", example = "ROLE_ADMIN")
    private UserRole role;
}
