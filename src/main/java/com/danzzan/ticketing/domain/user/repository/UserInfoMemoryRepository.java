package com.danzzan.ticketing.domain.user.repository;

import com.danzzan.ticketing.domain.user.model.UserInfo;

import java.util.Optional;

/**
 * 사용자 정보 캐시 저장소 인터페이스
 * 구현체를 변경하여 메모리/Redis 등 다양한 저장소 사용 가능
 */
public interface UserInfoMemoryRepository {

    /**
     * 캐시에서 사용자 정보 조회
     */
    Optional<UserInfo> getUserInfo(Long userId);

    /**
     * 캐시에 사용자 정보 저장
     */
    void setUserInfo(Long userId, UserInfo userInfo);

    /**
     * 캐시에서 사용자 정보 삭제
     */
    void removeUserInfo(Long userId);
}
