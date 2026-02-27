package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private static final String USER_CLAIMED_VALUE = "1";
    private static final int LUA_RESULT_SIZE = 2;
    private static final int LUA_CODE_INDEX = 0;
    private static final int LUA_REMAINING_INDEX = 1;
    private static final long LUA_CODE_ALREADY = 1L;
    private static final long LUA_CODE_SOLD_OUT = 2L;
    private static final long LUA_CODE_SUCCESS = 3L;

    private final StringRedisTemplate stringRedisTemplate;
    @Qualifier("claimV2Script")
    private final RedisScript<List> claimV2Script;

    @Override
    public ClaimResult claim(String eventId, String userId) {
        String userKey = TicketRedisKeys.userKey(eventId, userId);
        String stockKey = TicketRedisKeys.stockKey(eventId);
        String statusKey = TicketRedisKeys.statusKey(eventId, userId);

        List<?> rawResult = stringRedisTemplate.execute(
                claimV2Script,
                List.of(userKey, stockKey, statusKey),
                TicketRequestStatus.ALREADY.name(),
                TicketRequestStatus.SOLD_OUT.name(),
                TicketRequestStatus.SUCCESS.name(),
                USER_CLAIMED_VALUE,
                String.valueOf(LUA_CODE_ALREADY),
                String.valueOf(LUA_CODE_SOLD_OUT),
                String.valueOf(LUA_CODE_SUCCESS)
        );
        return mapLuaResult(rawResult);
    }

    private ClaimResult mapLuaResult(List<?> rawResult) {
        if (rawResult == null || rawResult.size() < LUA_RESULT_SIZE) {
            throw new IllegalStateException("claim lua result must contain [code, remaining]");
        }

        long code = asLong(rawResult.get(LUA_CODE_INDEX), "code");
        Long remaining = asNullableLong(rawResult.get(LUA_REMAINING_INDEX), "remaining");

        if (code == LUA_CODE_SUCCESS) {
            if (remaining == null) {
                throw new IllegalStateException("claim lua success code requires remaining value");
            }
            return ClaimResult.success(remaining);
        }

        if (code == LUA_CODE_SOLD_OUT) {
            return ClaimResult.soldOut();
        }

        if (code == LUA_CODE_ALREADY) {
            return ClaimResult.already();
        }

        throw new IllegalStateException("unexpected claim lua code: " + code);
    }

    private Long asNullableLong(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        return asLong(value, fieldName);
    }

    private long asLong(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(fieldName + " must be parseable as long", e);
            }
        }
        throw new IllegalStateException(fieldName + " must be number-like");
    }
}
