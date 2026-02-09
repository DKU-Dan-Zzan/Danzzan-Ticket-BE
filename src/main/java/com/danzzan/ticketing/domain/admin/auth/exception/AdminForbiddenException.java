package com.danzzan.ticketing.domain.admin.auth.exception;

public class AdminForbiddenException extends RuntimeException {
    public AdminForbiddenException() {
        super("관리자 권한이 없습니다.");
    }
}
