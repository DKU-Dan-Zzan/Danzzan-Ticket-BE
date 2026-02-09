package com.danzzan.ticketing.domain.admin.auth.exception;

public class AdminAuthenticationException extends RuntimeException {
    public AdminAuthenticationException() {
        super("관리자 인증 정보가 없습니다.");
    }
}
