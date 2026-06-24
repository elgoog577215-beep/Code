package com.onlinejudge.learning.standardlibrary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_standard_library_growth_candidates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStandardLibraryGrowthCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiStandardLibraryLayer layer;

    @Column(nullable = false, length = 120)
    private String suggestedCode;

    @Column(nullable = false, length = 160)
    private String suggestedName;

    @Column(length = 800)
    private String suggestedPath;

    private Long sourceProblemId;

    private Long sourceSubmissionId;

    @Column(length = 1200)
    private String evidenceRefs;

    @Column(length = 1200)
    private String similarExistingItems;

    @Column(length = 1600)
    private String changeReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiStandardLibraryGrowthCandidateStatus status;

    @Column(length = 1600)
    private String precheckMessage;

    private Double confidence;

    @Lob
    private String beforeSnapshot;

    @Lob
    private String diffSummary;

    @Lob
    private String rollbackInfo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = AiStandardLibraryGrowthCandidateStatus.PROPOSED;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
