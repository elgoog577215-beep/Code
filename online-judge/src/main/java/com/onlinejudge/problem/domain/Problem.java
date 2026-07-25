package com.onlinejudge.problem.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.onlinejudge.shared.persistence.StringListJsonConverter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "problems")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column
    @Builder.Default
    private ProblemStatus status = ProblemStatus.HIDDEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false)
    private Integer timeLimit; // in milliseconds

    @Column(nullable = false)
    private Integer memoryLimit; // in KB

    @Column(columnDefinition = "TEXT")
    private String aiPromptDirection;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "statement_background", columnDefinition = "TEXT")
    private String statementBackground;

    @Column(name = "statement_description", columnDefinition = "TEXT")
    private String statementDescription;

    @Column(name = "statement_input_format", columnDefinition = "TEXT")
    private String statementInputFormat;

    @Column(name = "statement_output_format", columnDefinition = "TEXT")
    private String statementOutputFormat;

    @Column(name = "statement_samples", columnDefinition = "TEXT")
    private String statementSamples;

    @Column(name = "statement_hints", columnDefinition = "TEXT")
    private String statementHints;

    @Column(length = 255)
    private String provider;

    @Column(columnDefinition = "TEXT")
    private String attachments;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags;

    @Column(name = "data_download_enabled")
    @Builder.Default
    private Boolean dataDownloadEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_display_mode")
    @Builder.Default
    private ScoreDisplayMode scoreDisplayMode = ScoreDisplayMode.ICPC;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "knowledge_points", columnDefinition = "TEXT")
    private List<String> knowledgePoints;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "algorithm_strategies", columnDefinition = "TEXT")
    private List<String> algorithmStrategies;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "common_mistakes", columnDefinition = "TEXT")
    private List<String> commonMistakes;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "boundary_types", columnDefinition = "TEXT")
    private List<String> boundaryTypes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public enum ProblemStatus {
        HIDDEN, PUBLIC, PARTIAL, CONTEST
    }

    public enum ScoreDisplayMode {
        OI, ICPC
    }
}
