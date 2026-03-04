package com.danzzan.ticketing.global.config;

import com.danzzan.ticketing.domain.user.model.entity.AcademicStatus;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.model.entity.UserRole;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private static final String LOADTEST_ENABLED_ENV = "LOADTEST_USERS_ENABLED";
    private static final String LOADTEST_COUNT_ENV = "LOADTEST_USER_COUNT";
    private static final String LOADTEST_PREFIX_ENV = "LOADTEST_USER_PREFIX";
    private static final String LOADTEST_PASSWORD_ENV = "LOADTEST_USER_PASSWORD";
    private static final String LOADTEST_COLLEGE_ENV = "LOADTEST_USER_COLLEGE";
    private static final String LOADTEST_MAJOR_ENV = "LOADTEST_USER_MAJOR";

    private static final String LOADTEST_DEFAULT_PREFIX = "loadtest-";
    private static final String LOADTEST_DEFAULT_PASSWORD = "loadtest1234!";
    private static final String LOADTEST_DEFAULT_COLLEGE = "SW융합대학";
    private static final String LOADTEST_DEFAULT_MAJOR = "소프트웨어학과";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminAccount();
        seedLoadtestUsersIfEnabled();
    }

    private void seedAdminAccount() {
        // 관리자 계정이 없으면 생성
        if (!userRepository.existsByStudentId("1234")) {
            User admin = User.builder()
                    .studentId("1234")
                    .password(passwordEncoder.encode("1234"))
                    .name("관리자")
                    .college("SW융합대학")
                    .major("소프트웨어학과")
                    .academicStatus(AcademicStatus.ENROLLED)
                    .role(UserRole.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("개발용 관리자 계정 생성 완료: studentId=1234, password=1234");
        }
    }

    private void seedLoadtestUsersIfEnabled() {
        boolean enabled = Boolean.parseBoolean(
                System.getenv().getOrDefault(LOADTEST_ENABLED_ENV, "false")
        );
        if (!enabled) {
            return;
        }

        int userCount = parseUserCount(System.getenv().getOrDefault(LOADTEST_COUNT_ENV, "0"));
        if (userCount <= 0) {
            log.warn(
                    "{}=true 이지만 {}가 1 이상이 아닙니다. 값={}",
                    LOADTEST_ENABLED_ENV,
                    LOADTEST_COUNT_ENV,
                    System.getenv(LOADTEST_COUNT_ENV)
            );
            return;
        }

        String studentIdPrefix = System.getenv().getOrDefault(LOADTEST_PREFIX_ENV, LOADTEST_DEFAULT_PREFIX);
        String password = System.getenv().getOrDefault(LOADTEST_PASSWORD_ENV, LOADTEST_DEFAULT_PASSWORD);
        String college = System.getenv().getOrDefault(LOADTEST_COLLEGE_ENV, LOADTEST_DEFAULT_COLLEGE);
        String major = System.getenv().getOrDefault(LOADTEST_MAJOR_ENV, LOADTEST_DEFAULT_MAJOR);
        String encodedPassword = passwordEncoder.encode(password);

        int created = 0;
        int existing = 0;
        for (int i = 1; i <= userCount; i++) {
            String studentId = studentIdPrefix + String.format("%06d", i);
            if (userRepository.existsByStudentId(studentId)) {
                existing++;
                continue;
            }

            User user = User.builder()
                    .studentId(studentId)
                    .password(encodedPassword)
                    .name("Loadtest User " + i)
                    .college(college)
                    .major(major)
                    .academicStatus(AcademicStatus.ENROLLED)
                    .role(UserRole.ROLE_USER)
                    .build();
            userRepository.save(user);
            created++;
        }

        log.info(
                "부하테스트 계정 시드 완료: requested={}, created={}, existing={}, prefix={}",
                userCount,
                created,
                existing,
                studentIdPrefix
        );
    }

    private int parseUserCount(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
