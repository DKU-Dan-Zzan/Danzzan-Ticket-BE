package com.danzzan.ticketing.domain.ticket.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class TicketRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private DefaultRedisScript<Long> reserveScript;

    // 잔여석 로컬 캐시: 1초 TTL, 2000명이 동시 폴링해도 Redis 호출은 초당 1회
    private final Cache<Long, Integer> remainingCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(100)
            .build();

    private static final String REMAINING_KEY = "ticket:event:%d:remaining";
    private static final String STATUS_KEY = "ticket:event:%d:status";
    private static final String USER_LOCK_KEY = "ticket:reserve:lock:%d:%d";
    private static final String LOCK_TTL = "3";

    @PostConstruct
    public void init() {
        reserveScript = new DefaultRedisScript<>();
        reserveScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/reserve_ticket.lua")));
        reserveScript.setResultType(Long.class);
    }

    /**
     * Lua 스크립트로 원자적 예매 시도
     * @return >=0 성공(newRemaining), -1 미오픈, -2 중복클릭, -3 매진
     */
    public long tryReserve(Long eventId, Long userId) {
        String remainingKey = String.format(REMAINING_KEY, eventId);
        String statusKey = String.format(STATUS_KEY, eventId);
        String lockKey = String.format(USER_LOCK_KEY, eventId, userId);

        List<String> keys = List.of(remainingKey, statusKey, lockKey);
        Long result = redisTemplate.execute(reserveScript, keys, LOCK_TTL);
        return result != null ? result : -1;
    }

    /**
     * DB INSERT 실패 시 잔여석 복원
     */
    public void restoreOne(Long eventId) {
        String remainingKey = String.format(REMAINING_KEY, eventId);
        redisTemplate.opsForValue().increment(remainingKey);
    }

    /**
     * 이벤트 오픈: Redis에 잔여수량 + 상태 SET
     */
    public void openEvent(Long eventId, int totalCapacity) {
        String remainingKey = String.format(REMAINING_KEY, eventId);
        String statusKey = String.format(STATUS_KEY, eventId);
        redisTemplate.opsForValue().set(remainingKey, String.valueOf(totalCapacity));
        redisTemplate.opsForValue().set(statusKey, "OPEN");
    }

    /**
     * 이벤트 마감: Redis 상태를 CLOSED로 변경
     */
    public void closeEvent(Long eventId) {
        String statusKey = String.format(STATUS_KEY, eventId);
        redisTemplate.opsForValue().set(statusKey, "CLOSED");
    }

    /**
     * 잔여석 조회 (폴링용) — Caffeine 로컬 캐시 (1초 TTL)
     * @return 잔여석 수, Redis에 키가 없으면 null
     */
    public Integer getRemaining(Long eventId) {
        Integer cached = remainingCache.getIfPresent(eventId);
        if (cached != null) {
            return cached;
        }
        String remainingKey = String.format(REMAINING_KEY, eventId);
        String value = redisTemplate.opsForValue().get(remainingKey);
        if (value != null) {
            int remaining = Integer.parseInt(value);
            remainingCache.put(eventId, remaining);
            return remaining;
        }
        return null;
    }

    /**
     * Redis 상태 조회
     */
    public String getStatus(Long eventId) {
        String statusKey = String.format(STATUS_KEY, eventId);
        return redisTemplate.opsForValue().get(statusKey);
    }
}
