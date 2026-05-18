package com.onlinejudge.classroom.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "class_group_id")
    private Long classGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hint_policy", nullable = false)
    private HintPolicy hintPolicy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (hintPolicy == null) {
            hintPolicy = HintPolicy.L2;
        }
        if (status == null) {
            status = AssignmentStatus.ACTIVE;
        }
    }

    public enum HintPolicy {
        L1, L2, L3, L4
    }

    public enum AssignmentStatus {
        DRAFT, ACTIVE, CLOSED
    }
}
