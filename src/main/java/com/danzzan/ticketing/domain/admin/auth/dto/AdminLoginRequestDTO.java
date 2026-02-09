package com.danzzan.ticketing.domain.admin.auth.dto;

import com.danzzan.ticketing.domain.user.model.entity.SystemType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "관리자 로그인 요청")
public class AdminLoginRequestDTO {

    @NotNull
    @Schema(description = "시스템 타입", example = "DANSPOT")
    private SystemType system;

    @NotBlank
    @Schema(description = "관리자 학번", example = "32221902")
    private String studentId;

    @NotBlank
    @Schema(description = "비밀번호", example = "admin_password")
    private String password;
}
