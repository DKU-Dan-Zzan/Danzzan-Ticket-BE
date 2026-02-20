package com.danzzan.ticketing.global.exception;

import com.danzzan.ticketing.global.exception.AdminAuthenticationException;
import com.danzzan.ticketing.global.exception.AdminForbiddenException;
import com.danzzan.ticketing.domain.event.exception.EventNotFoundException;
import com.danzzan.ticketing.domain.ticket.exception.*;
import com.danzzan.ticketing.domain.user.exception.AlreadyStudentIdException;
import com.danzzan.ticketing.domain.user.exception.UserNotFoundException;
import com.danzzan.ticketing.domain.user.exception.WrongPasswordException;
import com.danzzan.ticketing.global.model.ApiError;
import com.danzzan.ticketing.global.model.ApiResponse;
import com.danzzan.ticketing.infra.dku.exception.DkuFailedCrawlingException;
import com.danzzan.ticketing.infra.dku.exception.DkuFailedLoginException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AlreadyStudentIdException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyStudentId(AlreadyStudentIdException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ResponseEntity<Map<String, String>> handleWrongPassword(WrongPasswordException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTicketNotFound(TicketNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("TICKET_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEventNotFound(EventNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("EVENT_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(TicketEventMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTicketEventMismatch(TicketEventMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("EVENT_MISMATCH", e.getMessage()));
    }

    @ExceptionHandler(TicketAlreadyIssuedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTicketAlreadyIssued(TicketAlreadyIssuedException e) {
        ApiError error = ApiError.builder()
                .error("ALREADY_ISSUED")
                .message(e.getMessage())
                .ticketId(e.getTicketId())
                .issuedAt(e.getIssuedAt())
                .issuerAdminName(e.getIssuerAdminName())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .data(null)
                        .error(error)
                        .build());
    }

    @ExceptionHandler(TicketNotIssuedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTicketNotIssued(TicketNotIssuedException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("NOT_ISSUED", e.getMessage()));
    }

    // 티켓팅: 오픈 전 예매 시도
    @ExceptionHandler(EventNotOpenException.class)
    public ResponseEntity<Map<String, String>> handleEventNotOpen(EventNotOpenException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    // 티켓팅: 정원 초과 (입력 중 마감)
    @ExceptionHandler(EventSoldOutException.class)
    public ResponseEntity<Map<String, String>> handleEventSoldOut(EventSoldOutException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    // 티켓팅: 이미 예매한 공연
    @ExceptionHandler(AlreadyReservedException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyReserved(AlreadyReservedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AdminAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminAuthentication(AdminAuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", e.getMessage()));
    }

    @ExceptionHandler(AdminForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdminForbidden(AdminForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(DkuFailedLoginException.class)
    public ResponseEntity<Map<String, String>> handleDkuFailedLogin(DkuFailedLoginException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DkuFailedCrawlingException.class)
    public ResponseEntity<Map<String, String>> handleDkuFailedCrawling(DkuFailedCrawlingException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }
}
