package com.danzzan.ticketing.infra.dku.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 단국대 포털에서 가져온 학생 정보
// DkuStudentService에서 크롤링한 데이터를 담는 DTO
@Getter
@RequiredArgsConstructor
public class StudentInfo {

    private final String studentName;       // 학생 이름
    private final String studentId;         // 학번
    private final String college;           // 단과대학
    private final String major;             // 학과
    private final String academicStatus;    // 학적상태 (재학, 휴학, 졸업 등)
    private final int yearOfAdmission;      // 입학년도
}
