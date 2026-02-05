package com.danzzan.ticketing.domain.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RequestRefreshTokenDto {

    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private final String refreshToken;
}
