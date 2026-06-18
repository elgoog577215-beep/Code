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
}
