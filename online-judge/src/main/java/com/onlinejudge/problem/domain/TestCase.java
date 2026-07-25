package com.onlinejudge.problem.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String input;

    @Column(name = "expected_output", columnDefinition = "TEXT", nullable = false)
    private String expectedOutput;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_storage_type")
    @Builder.Default
    private StorageType inputStorageType = StorageType.INLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_storage_type")
    @Builder.Default
    private StorageType outputStorageType = StorageType.INLINE;

    @Column(name = "input_file_path", columnDefinition = "TEXT")
    private String inputFilePath;

    @Column(name = "output_file_path", columnDefinition = "TEXT")
    private String outputFilePath;

    @Column(name = "input_file_name")
    private String inputFileName;

    @Column(name = "output_file_name")
    private String outputFileName;

    @Column(name = "input_size_bytes")
    private Long inputSizeBytes;

    @Column(name = "output_size_bytes")
    private Long outputSizeBytes;

    @Column(name = "input_sha256", length = 64)
    private String inputSha256;

    @Column(name = "output_sha256", length = 64)
    private String outputSha256;

    @Column(name = "time_limit_ms")
    private Integer timeLimitMs;

    @Column(name = "memory_limit_kib")
    private Integer memoryLimitKib;

    @Column(name = "subtask_index")
    @Builder.Default
    private Integer subtaskIndex = 0;

    @Column(name = "score")
    @Builder.Default
    private Integer score = 0;

    @Column(name = "public_example")
    @Builder.Default
    private Boolean publicExample = false;

    @Column(name = "import_batch_id", length = 80)
    private String importBatchId;

    @Column(name = "is_hidden")
    @Builder.Default
    private Boolean isHidden = false;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "semantic_code", length = 160)
    private String semanticCode;

    @Column(name = "intent_type", length = 40)
    private String intentType;

    @Column(name = "intent_title", length = 160)
    private String intentTitle;

    @Column(name = "intent_summary", length = 800)
    private String intentSummary;

    @Column(name = "learning_objective", length = 800)
    private String learningObjective;

    @Column(name = "contest_role", length = 40)
    private String contestRole;

    @Column(name = "reveal_policy", length = 40)
    private String revealPolicy;

    @Column(name = "knowledge_node_code", length = 160)
    private String knowledgeNodeCode;

    @Column(name = "skill_unit_code", length = 160)
    private String skillUnitCode;

    @Column(name = "review_status", length = 40)
    private String reviewStatus;

    @Column(name = "source_reference", length = 1200)
    private String sourceReference;

    @Column(name = "library_version", length = 80)
    private String libraryVersion;

    @Column(name = "reviewed_at")
    private java.time.LocalDateTime reviewedAt;

    public enum StorageType {
        INLINE, FILE
    }
}
