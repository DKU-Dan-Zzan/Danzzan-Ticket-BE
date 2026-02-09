package com.danzzan.ticketing.domain.event.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException() {
        super("해당 공연을 찾을 수 없습니다.");
    }
}
