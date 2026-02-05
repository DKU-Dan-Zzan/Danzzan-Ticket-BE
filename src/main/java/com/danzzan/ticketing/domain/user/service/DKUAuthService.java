package com.danzzan.ticketing.domain.user.service;

import com.danzzan.ticketing.domain.user.exception.AlreadyStudentIdException;
import com.danzzan.ticketing.domain.user.model.dto.request.RequestDkuStudentDto;
import com.danzzan.ticketing.domain.user.model.dto.response.ResponseScrappedStudentInfoDto;
import com.danzzan.ticketing.domain.user.model.dto.response.ResponseVerifyStudentDto;
import com.danzzan.ticketing.domain.user.model.entity.AcademicStatus;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import com.danzzan.ticketing.infra.dku.DkuAuthenticationService;
import com.danzzan.ticketing.infra.dku.DkuStudentService;
import com.danzzan.ticketing.infra.dku.model.DkuAuth;
import com.danzzan.ticketing.infra.dku.model.StudentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DKUAuthService {

    private final UserRepository userRepository;
    private final SignupService signupService;
    private final DkuAuthenticationService dkuAuthenticationService;
    private final DkuStudentService dkuStudentService;

    // 단국대 포털을 통해 학생 인증 진행
    // 1. 이미 가입된 학번인지 확인
    // 2. 단국대 포털에 로그인하여 학생 정보 크롤링
    // 3. 재학생인지 확인 후 회원가입 토큰 발급
    public ResponseVerifyStudentDto verifyStudent(RequestDkuStudentDto dto) {
        // 이미 가입된 학번인지 체크
        if (userRepository.existsByStudentId(dto.getDkuStudentId())) {
            throw new AlreadyStudentIdException();
        }

        // 단국대 포털 로그인
        DkuAuth auth = dkuAuthenticationService.login(dto.getDkuStudentId(), dto.getDkuPassword());

        // 학생 정보 크롤링
        StudentInfo studentInfo = dkuStudentService.crawlStudentInfo(auth);

        // 학적 상태 변환
        AcademicStatus academicStatus = parseAcademicStatus(studentInfo.getAcademicStatus());

        // 재학생만 가입 가능
        if (academicStatus != AcademicStatus.ENROLLED) {
            throw new IllegalStateException("재학생만 회원가입이 가능합니다.");
        }

        // 회원가입 토큰 생성
        String signupToken = UUID.randomUUID().toString();

        // 학생 정보 캐시에 저장
        signupService.cacheStudentInfo(
                signupToken,
                studentInfo.getStudentId(),
                studentInfo.getStudentName(),
                studentInfo.getCollege(),
                studentInfo.getMajor(),
                academicStatus
        );

        // 응답 DTO 생성
        ResponseScrappedStudentInfoDto studentDto = new ResponseScrappedStudentInfoDto(
                studentInfo.getStudentName(),
                studentInfo.getStudentId(),
                studentInfo.getCollege(),
                studentInfo.getMajor()
        );

        return new ResponseVerifyStudentDto(signupToken, studentDto);
    }

    // 회원가입 토큰으로 캐시에서 학생 정보 조회
    public ResponseScrappedStudentInfoDto getStudentInfo(String signupToken) {
        SignupService.StudentInfoCache cache = signupService.getCachedStudentInfo(signupToken);
        return new ResponseScrappedStudentInfoDto(
                cache.name(), cache.studentId(), cache.college(), cache.major()
        );
    }

    // 학적 상태 문자열을 AcademicStatus enum으로 변환
    // "재학", "휴학", "졸업" 등의 문자열을 파싱
    private AcademicStatus parseAcademicStatus(String status) {
        if (status == null || status.isEmpty()) {
            return AcademicStatus.ENROLLED;
        }

        String normalized = status.trim();

        if (normalized.contains("재학") || normalized.contains("재籍") || normalized.equals("ENROLLED")) {
            return AcademicStatus.ENROLLED;
        } else if (normalized.contains("휴학") || normalized.equals("LEAVE")) {
            return AcademicStatus.LEAVE;
        } else if (normalized.contains("졸업") || normalized.equals("GRADUATED")) {
            return AcademicStatus.GRADUATED;
        }

        // 알 수 없는 상태는 일단 재학으로 처리
        log.warn("알 수 없는 학적 상태: {}", status);
        return AcademicStatus.ENROLLED;
    }
}
