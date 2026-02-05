package com.danzzan.ticketing.domain.user.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "회원가입 완료 요청")
public class RequestSignupDto {

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 4, max = 200, message = "비밀번호는 4~200자여야 합니다")
    @Schema(description = "비밀번호", example = "mypassword123")
    private final String password;
}
