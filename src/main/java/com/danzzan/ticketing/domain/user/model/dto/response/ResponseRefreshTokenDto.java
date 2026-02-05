package com.danzzan.ticketing.domain.user.model.dto.response;

import lombok.Getter;

@Getter
public class ResponseRefreshTokenDto {

    private final String accessToken;
    private final String refreshToken;

    public ResponseRefreshTokenDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
