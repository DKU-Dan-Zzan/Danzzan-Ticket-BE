package com.danzzan.ticketing.domain.user.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "단국대 학생 인증 요청")
public class RequestDkuStudentDto {

    @NotBlank(message = "학번은 필수입니다")
    @Schema(description = "단국대 학번", example = "32100000")
    private final String dkuStudentId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Schema(description = "단국대 포털 비밀번호", example = "dku_password")
    private final String dkuPassword;
}
