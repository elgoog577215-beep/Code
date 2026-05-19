package com.onlinejudge.classroom.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_recommendation_events",
        indexes = {
                @Index(name = "idx_reco_events_student", columnList = "student_profile_id, created_at"),
                @Index(name = "idx_reco_events_token", columnList = "recommendation_token"),
                @Index(name = "idx_reco_events_submission", columnList = "followup_submission_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRecommendationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recommendation_token", nullable = false)
    private String recommendationToken;

    @Column(name = "student_profile_id", nullable = false)
    private Long studentProfileId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "problem_id")
    private Long problemId;

    @Column(name = "focus_ability")
    private String focusAbility;

    @Column(name = "focus_tags", columnDefinition = "TEXT")
    private String focusTags;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "followup_submission_id")
    private Long followupSubmissionId;

    @Column(name = "followup_verdict")
    private String followupVerdict;

    @Column(name = "followup_issue_tag")
    private String followupIssueTag;

    @Column(name = "followup_fine_grained_tag")
    private String followupFineGrainedTag;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
