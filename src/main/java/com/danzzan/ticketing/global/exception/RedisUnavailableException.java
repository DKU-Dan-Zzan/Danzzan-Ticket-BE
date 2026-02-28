package com.danzzan.ticketing.global.exception;

public class RedisUnavailableException extends RuntimeException {
    public RedisUnavailableException() {
        super("서버가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.");
    }

    public RedisUnavailableException(String message) {
        super(message);
    }
}
