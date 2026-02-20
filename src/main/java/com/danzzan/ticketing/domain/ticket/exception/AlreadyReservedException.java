package com.danzzan.ticketing.domain.ticket.exception;

public class AlreadyReservedException extends RuntimeException {
    public AlreadyReservedException() {
        super("이미 예매한 공연입니다. 내 티켓에서 확인해주세요.");
    }

    public AlreadyReservedException(String message) {
        super(message);
    }
}
