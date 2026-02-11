package com.danzzan.ticketing.domain.auth.controller;

import com.danzzan.ticketing.domain.auth.dto.RequestDkuStudentDto;
import com.danzzan.ticketing.domain.auth.dto.ResponseScrappedStudentInfoDto;
import com.danzzan.ticketing.domain.auth.dto.ResponseVerifyStudentDto;
import com.danzzan.ticketing.domain.auth.service.DKUAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/dku")
@RequiredArgsConstructor
@Tag(name = "1단계: 단국대 학생 인증", description = "단국대 포털을 통한 학생 인증 API")
public class DKUController {

    private final DKUAuthService dkuAuthService;

    @PostMapping("/verify")
    @Operation(summary = "학생 인증", description = "단국대 포털 ID/PW로 학생 인증 후 회원가입 토큰 발급")
    public ResponseEntity<ResponseVerifyStudentDto> verifyStudent(
            @Valid @RequestBody RequestDkuStudentDto dto) {
        ResponseVerifyStudentDto response = dkuAuthService.verifyStudent(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{signup-token}")
    @Operation(summary = "학생 정보 조회", description = "회원가입 토큰으로 인증된 학생 정보 조회")
    public ResponseEntity<ResponseScrappedStudentInfoDto> getStudentInfo(
            @PathVariable("signup-token") String signupToken) {
        ResponseScrappedStudentInfoDto response = dkuAuthService.getStudentInfo(signupToken);
        return ResponseEntity.ok(response);
    }
}
