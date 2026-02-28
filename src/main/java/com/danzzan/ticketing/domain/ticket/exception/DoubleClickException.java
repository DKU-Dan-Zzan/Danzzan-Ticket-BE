package com.danzzan.ticketing.domain.ticket.exception;

public class DoubleClickException extends RuntimeException {
    public DoubleClickException() {
        super("요청이 처리 중입니다. 잠시 후 다시 시도해주세요.");
    }
}
