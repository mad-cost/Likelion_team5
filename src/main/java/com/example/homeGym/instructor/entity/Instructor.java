package com.example.homeGym.instructor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Instructor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String loginId;
    private String password;
    @Enumerated(EnumType.STRING)
    @Setter
    private Gender gender;
    private String name;
    private String birthyear;
    private String birthday;
    @Setter
    @Enumerated(EnumType.STRING)
    private InstructorState state;
    @Setter
    private String career;
    @Setter
    private String profileImageUrl;
    @Setter
    private String certificate;
    @Setter
    private String medal;
    @Setter
    private String email;
    @Setter
    private String phone;
    @Setter
    private String bank;
    @Setter
    private String bankName;
    @Setter
    private Integer account;
    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum InstructorState{
        REGISTRATION_PENDING, WITHDRAWAL_PENDING, ACTIVE, WITHDRAWAL_COMPLETE
    }

}