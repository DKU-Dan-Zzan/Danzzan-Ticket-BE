package com.danzzan.ticketing.domain.user.controller;

import com.danzzan.ticketing.domain.user.model.dto.request.RequestLoginDto;
import com.danzzan.ticketing.domain.user.model.dto.request.RequestRefreshTokenDto;
import com.danzzan.ticketing.domain.auth.dto.RequestSignupDto;
import com.danzzan.ticketing.domain.user.model.dto.response.ResponseLoginDto;
import com.danzzan.ticketing.domain.user.model.dto.response.ResponseRefreshTokenDto;
import com.danzzan.ticketing.domain.auth.service.SignupService;
import com.danzzan.ticketing.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "2단계: 회원가입/로그인", description = "회원가입 완료 및 로그인 API")
public class UserController {

    private final UserService userService;
    private final SignupService signupService;

    @PostMapping("/{signup-token}")
    @Operation(summary = "회원가입 완료", description = "1단계에서 받은 토큰으로 비밀번호를 설정하여 회원가입 완료")
    public ResponseEntity<Void> signup(
            @PathVariable("signup-token") String signupToken,
            @Valid @RequestBody RequestSignupDto dto) {
        signupService.signup(dto, signupToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "학번과 비밀번호로 로그인")
    public ResponseEntity<ResponseLoginDto> login(@Valid @RequestBody RequestLoginDto dto) {
        ResponseLoginDto response = userService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "Access Token 만료 시 Refresh Token으로 새 토큰 발급")
    public ResponseEntity<ResponseRefreshTokenDto> refreshToken(
            HttpServletRequest request,
            @Valid @RequestBody RequestRefreshTokenDto dto) {
        String accessToken = extractAccessToken(request);
        ResponseRefreshTokenDto response = userService.refreshToken(accessToken, dto.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
