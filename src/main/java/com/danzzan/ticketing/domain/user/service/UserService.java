package com.danzzan.ticketing.domain.user.service;

import com.danzzan.ticketing.domain.user.exception.UserNotFoundException;
import com.danzzan.ticketing.domain.user.exception.WrongPasswordException;
import com.danzzan.ticketing.domain.user.model.dto.request.RequestLoginDto;
import com.danzzan.ticketing.domain.user.model.dto.response.ResponseLoginDto;
import com.danzzan.ticketing.domain.user.model.dto.response.ResponseRefreshTokenDto;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
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

    /**
     * 로그인
     */
    public ResponseLoginDto login(RequestLoginDto dto) {
        User user = userRepository.findByStudentId(dto.getStudentId())
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new WrongPasswordException();
        }

        // 로그인 성공 시 사용자 정보 캐시에 저장
        userInfoService.cacheUserInfo(user.getId(), user);

        // TODO: JWT 토큰 발급 구현 필요
        return new ResponseLoginDto("access-token-temp", "refresh-token-temp");
    }

    /**
     * 토큰 재발급
     */
    public ResponseRefreshTokenDto refreshToken(String accessToken, String refreshToken) {
        // TODO: JWT 토큰 재발급 구현 필요
        return new ResponseRefreshTokenDto("new-access-token-temp", "new-refresh-token-temp");
    }
}
