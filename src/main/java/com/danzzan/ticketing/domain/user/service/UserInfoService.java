package com.danzzan.ticketing.domain.user.service;

import com.danzzan.ticketing.domain.user.exception.UserNotFoundException;
import com.danzzan.ticketing.domain.user.model.UserInfo;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.repository.UserInfoMemoryRepository;
import com.danzzan.ticketing.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 사용자 정보 캐싱 서비스
// DB 조회를 줄이기 위해 메모리에 사용자 정보 캐싱
@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserRepository userRepository;
    private final UserInfoMemoryRepository memoryRepository;

    // 사용자 정보 조회 (캐시 우선)
    // 캐시에 있으면 캐시에서 반환, 없으면 DB 조회 후 캐시에 저장
    @Transactional(readOnly = true)
    public UserInfo getUserInfo(Long userId) {
        return memoryRepository.getUserInfo(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(UserNotFoundException::new);
                    UserInfo userInfo = new UserInfo(user);
                    memoryRepository.setUserInfo(userId, userInfo);
                    return userInfo;
                });
    }

    // 사용자 정보 캐시에 저장 (로그인 성공 시 호출)
    public void cacheUserInfo(Long userId, User user) {
        UserInfo userInfo = new UserInfo(user);
        memoryRepository.setUserInfo(userId, userInfo);
    }

    // 사용자 정보 캐시 삭제 (정보 변경 시 호출)
    public void invalidateUserInfo(Long userId) {
        memoryRepository.removeUserInfo(userId);
    }
}
