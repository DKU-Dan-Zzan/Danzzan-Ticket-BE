package com.danzzan.ticketing.domain.admin.auth.controller;

import com.danzzan.ticketing.global.model.ApiResponse;
import com.danzzan.ticketing.domain.admin.auth.dto.AdminLoginRequestDTO;
import com.danzzan.ticketing.domain.admin.auth.dto.AdminLoginResponseDTO;
import com.danzzan.ticketing.domain.admin.auth.dto.AdminLogoutResponseDTO;
import com.danzzan.ticketing.domain.admin.auth.dto.AdminMeResponseDTO;
import com.danzzan.ticketing.domain.admin.auth.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "관리자 인증", description = "관리자 로그인/로그아웃/내 정보 API")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/auth/login")
    @Operation(
            summary = "관리자 로그인",
            description = "관리자 학번과 비밀번호로 로그인 후 임시 토큰 발급"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "accessToken": "admin-access-token-temp-123e4567-e89b-12d3-a456-426614174000",
                                                "admin": {
                                                  "id": 5,
                                                  "name": "김관리",
                                                  "studentId": "32221902",
                                                  "role": "ROLE_ADMIN"
                                                },
                                                "system": "WRISTBAND"
                                              },
                                              "error": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패(비밀번호 오류)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "관리자 계정 없음")
    })
    public ApiResponse<AdminLoginResponseDTO> login(@Valid @RequestBody AdminLoginRequestDTO request) {
        return ApiResponse.success(adminAuthService.login(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/auth/me")
    @Operation(
            summary = "현재 로그인한 관리자 정보 조회",
            description = "Authorization 헤더의 임시 토큰으로 관리자 계정 정보 반환"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "adminId": 5,
                                                "adminName": "김관리",
                                                "studentId": "32221902",
                                                "role": "ROLE_ADMIN"
                                              },
                                              "error": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiResponse<AdminMeResponseDTO> me(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);
        return ApiResponse.success(adminAuthService.me(accessToken));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/auth/logout")
    @Operation(
            summary = "관리자 로그아웃",
            description = "임시 토큰을 무효화하여 로그아웃 처리"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "ok": true
                                              },
                                              "error": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ApiResponse<AdminLogoutResponseDTO> logout(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);
        return ApiResponse.success(adminAuthService.logout(accessToken));
    }

    private String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
