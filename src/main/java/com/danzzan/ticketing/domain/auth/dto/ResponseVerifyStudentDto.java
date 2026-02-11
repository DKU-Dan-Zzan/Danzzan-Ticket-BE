package com.danzzan.ticketing.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "단국대 학생 인증 응답")
public class ResponseVerifyStudentDto {

    @Schema(description = "회원가입용 토큰", example = "550e8400-e29b-41d4-a716-446655440000")
    private final String signupToken;

    @Schema(description = "학생 정보")
    private final ResponseScrappedStudentInfoDto student;

    public ResponseVerifyStudentDto(String signupToken, ResponseScrappedStudentInfoDto student) {
        this.signupToken = signupToken;
        this.student = student;
    }
}
