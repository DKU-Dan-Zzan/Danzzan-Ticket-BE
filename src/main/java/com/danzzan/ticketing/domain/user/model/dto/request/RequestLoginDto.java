package com.danzzan.ticketing.domain.user.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "로그인 요청")
public class RequestLoginDto {

    @NotBlank(message = "학번은 필수입니다")
    @Schema(description = "학번", example = "32100000")
    private final String studentId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Schema(description = "비밀번호", example = "mypassword123")
    private final String password;
}
