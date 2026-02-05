package com.danzzan.ticketing.domain.user.repository.impl;

import com.danzzan.ticketing.domain.user.model.UserInfo;
import com.danzzan.ticketing.domain.user.repository.UserInfoMemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자 정보 캐시 - 메모리 구현체
 * 서버 재시작 시 캐시가 초기화됨
 * 추후 Redis로 변경 가능
 */
@Repository
public class UserInfoMemoryRepositoryImpl implements UserInfoMemoryRepository {

    private final Map<Long, UserInfo> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<UserInfo> getUserInfo(Long userId) {
        return Optional.ofNullable(cache.get(userId));
    }

    @Override
    public void setUserInfo(Long userId, UserInfo userInfo) {
        cache.put(userId, userInfo);
    }

    @Override
    public void removeUserInfo(Long userId) {
        cache.remove(userId);
    }
}
