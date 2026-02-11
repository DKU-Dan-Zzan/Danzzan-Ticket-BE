package com.danzzan.ticketing.domain.user.service;

import com.danzzan.ticketing.domain.user.exception.UserNotFoundException;
import com.danzzan.ticketing.domain.user.exception.WrongPasswordException;
import com.danzzan.ticketing.domain.user.model.dto.request.RequestLoginDto;
import com.danzzan.ticketing.domain.user.model.dto.response.ResponseLoginDto;
import com.danzzan.ticketing.domain.user.model.dto.response.ResponseRefreshTokenDto;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import com.danzzan.ticketing.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserInfoService userInfoService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 로그인 처리
    // 학번으로 사용자 조회 후 비밀번호 검증, JWT 토큰 발급
    public ResponseLoginDto login(RequestLoginDto dto) {
        User user = userRepository.findByStudentId(dto.getStudentId())
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new WrongPasswordException();
        }

        // 로그인 성공 시 사용자 정보 캐시에 저장
        userInfoService.cacheUserInfo(user.getId(), user);

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getStudentId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return new ResponseLoginDto(accessToken, refreshToken);
    }

    // 토큰 재발급
    // 만료된 Access Token에서 userId를 추출하고, 유효한 Refresh Token이면 새 토큰 발급
    public ResponseRefreshTokenDto refreshToken(String accessToken, String refreshToken) {
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 만료된 Access Token에서 userId 추출
        Long userId = jwtTokenProvider.getClaimsFromExpiredToken(accessToken).getSubject() != null
                ? Long.parseLong(jwtTokenProvider.getClaimsFromExpiredToken(accessToken).getSubject())
                : jwtTokenProvider.getUserId(refreshToken);

        // userId로 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getStudentId(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return new ResponseRefreshTokenDto(newAccessToken, newRefreshToken);
    }
}
