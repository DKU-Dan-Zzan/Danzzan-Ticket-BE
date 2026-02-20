package com.danzzan.ticketing.domain.ticket.exception;

public class EventSoldOutException extends RuntimeException {
    public EventSoldOutException() {
        super("입력 중 정원이 마감되어 이번 신청은 완료되지 않았습니다.");
    }
}
