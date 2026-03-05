package com.danzzan.ticketing.domain.user.model.dto.response;

import lombok.Getter;

@Getter
public class ResponseLoginDto {

    private final String accessToken;
    private final String refreshToken;
    private final UserInfo user;

    public ResponseLoginDto(String accessToken, String refreshToken, UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    @Getter
    public static class UserInfo {
        private final String id;
        private final String studentId;
        private final String name;
        private final String role;
        private final String department;

        private final String college;

        public UserInfo(String id, String studentId, String name, String role, String department, String college) {
            this.id = id;
            this.studentId = studentId;
            this.name = name;
            this.role = role;
            this.department = department;
            this.college = college;
        }
    }
}
