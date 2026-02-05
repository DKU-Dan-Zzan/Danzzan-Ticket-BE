package com.danzzan.ticketing.domain.user.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String college;

    @Column(nullable = false)
    private String major;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_status", nullable = false)
    private AcademicStatus academicStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public User(String studentId, String password, String name,
                String college, String major, AcademicStatus academicStatus, UserRole role) {
        this.studentId = studentId;
        this.password = password;
        this.name = name;
        this.college = college;
        this.major = major;
        this.academicStatus = academicStatus;
        this.role = role != null ? role : UserRole.ROLE_USER;
        this.createdAt = LocalDateTime.now();
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }
}
