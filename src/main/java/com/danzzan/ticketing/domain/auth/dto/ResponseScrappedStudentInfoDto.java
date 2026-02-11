package com.danzzan.ticketing.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "단국대 학생 인증 결과 - 학생 정보")
public class ResponseScrappedStudentInfoDto {

    @Schema(description = "학생 이름", example = "홍길동")
    private final String studentName;

    @Schema(description = "학번", example = "32100000")
    private final String studentId;

    @Schema(description = "단과대학", example = "공과대학")
    private final String college;

    @Schema(description = "학과", example = "컴퓨터공학과")
    private final String major;

    public ResponseScrappedStudentInfoDto(String studentName, String studentId,
                                           String college, String major) {
        this.studentName = studentName;
        this.studentId = studentId;
        this.college = college;
        this.major = major;
    }
}
