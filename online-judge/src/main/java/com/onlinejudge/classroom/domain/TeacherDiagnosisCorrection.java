package com.onlinejudge.classroom.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "teacher_diagnosis_corrections",
        indexes = {
                @Index(name = "idx_teacher_corrections_assignment", columnList = "assignment_id, corrected_at"),
                @Index(name = "idx_teacher_corrections_submission", columnList = "submission_id, corrected_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDiagnosisCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "student_profile_id")
    private Long studentProfileId;

    @Column(name = "original_issue_tag")
    private String originalIssueTag;

    @Column(name = "original_fine_grained_tag")
    private String originalFineGrainedTag;

    @Column(name = "corrected_issue_tag", nullable = false)
    private String correctedIssueTag;

    @Column(name = "corrected_fine_grained_tag")
    private String correctedFineGrainedTag;

    @Column(name = "teacher_note", columnDefinition = "TEXT")
    private String teacherNote;

    @Column(name = "eval_candidate", nullable = false)
    private boolean evalCandidate;

    @Column(name = "corrected_by")
    private String correctedBy;

    @Column(name = "corrected_at", nullable = false)
    private LocalDateTime correctedAt;

    @PrePersist
    protected void onCreate() {
        correctedAt = LocalDateTime.now();
    }
}
