package com.danzzan.ticketing.domain.ticket.exception;

public class EventNotOpenException extends RuntimeException {
    public EventNotOpenException() {
        super("아직 예매가 시작되지 않은 공연입니다. 오픈 시간을 확인해주세요.");
    }
}
