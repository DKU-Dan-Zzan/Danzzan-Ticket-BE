package com.danzzan.ticketing.domain.user.model;

import com.danzzan.ticketing.domain.user.model.entity.AcademicStatus;
import com.danzzan.ticketing.domain.user.model.entity.User;
import com.danzzan.ticketing.domain.user.model.entity.UserRole;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class UserInfo {

    private final Long id;
    private final String studentId;
    private final String name;
    private final String college;
    private final String major;
    private final AcademicStatus academicStatus;
    private final UserRole role;

    public UserInfo(User user) {
        this.id = user.getId();
        this.studentId = user.getStudentId();
        this.name = user.getName();
        this.college = user.getCollege();
        this.major = user.getMajor();
        this.academicStatus = user.getAcademicStatus();
        this.role = user.getRole();
    }
}
