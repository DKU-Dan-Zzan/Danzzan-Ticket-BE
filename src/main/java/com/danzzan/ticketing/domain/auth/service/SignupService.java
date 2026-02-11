package com.danzzan.ticketing.domain.auth.service;

import com.danzzan.ticketing.domain.user.exception.AlreadyStudentIdException;
import com.danzzan.ticketing.domain.auth.dto.RequestSignupDto;
import com.danzzan.ticketing.domain.user.model.entity.AcademicStatus;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.model.entity.UserRole;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // TODO: Redis로 교체 예정
    private final Map<String, StudentInfoCache> signupCache = new ConcurrentHashMap<>();

    // 학생 정보 임시 저장
    public void cacheStudentInfo(String signupToken, String studentId, String name,
                                  String college, String major, AcademicStatus academicStatus) {
        signupCache.put(signupToken, new StudentInfoCache(studentId, name, college, major, academicStatus));
    }

    // 캐시에서 학생 정보 조회
    public StudentInfoCache getCachedStudentInfo(String signupToken) {
        StudentInfoCache cache = signupCache.get(signupToken);
        if (cache == null) {
            throw new IllegalArgumentException("유효하지 않은 회원가입 토큰입니다.");
        }
        return cache;
    }

    // 회원가입 처리
    @Transactional
    public void signup(RequestSignupDto dto, String signupToken) {
        StudentInfoCache cache = getCachedStudentInfo(signupToken);

        // 학번 중복 체크
        if (userRepository.existsByStudentId(cache.studentId())) {
            throw new AlreadyStudentIdException();
        }

        // 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        User user = User.builder()
                .studentId(cache.studentId())
                .password(encodedPassword)
                .name(cache.name())
                .college(cache.college())
                .major(cache.major())
                .academicStatus(cache.academicStatus())
                .role(UserRole.ROLE_USER)
                .build();

        userRepository.save(user);
        signupCache.remove(signupToken);
    }

    public record StudentInfoCache(
            String studentId,
            String name,
            String college,
            String major,
            AcademicStatus academicStatus
    ) {}
}
