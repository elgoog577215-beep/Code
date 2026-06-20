package com.onlinejudge.learning.standardlibrary.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryEmbedding;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryEmbeddingRepository;
import com.onlinejudge.submission.application.EmbeddingClient;
import com.onlinejudge.submission.application.EmbeddingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiStandardLibraryEmbeddingMaintenanceService {

    private static final int MAX_REBUILD_PER_TICK = 8;

    private final AiStandardLibraryService standardLibraryService;
    private final AiStandardLibraryEmbeddingRepository embeddingRepository;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties embeddingProperties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${ai.embedding.rebuild-delay-ms:60000}",
            initialDelayString = "${ai.embedding.initial-delay-ms:15000}")
    @Transactional
    public void rebuildStaleEmbeddings() {
        if (!embeddingProperties.isEnabled()) {
            return;
        }
        String model = normalizeModel();
        ensureEnabledItemsHaveEmbeddingRows(model);
        List<AiStandardLibraryEmbedding> staleEmbeddings =
                embeddingRepository.findByEmbeddingModelAndStatusOrderByUpdatedAtAsc(model, "STALE").stream()
                        .limit(MAX_REBUILD_PER_TICK)
                        .toList();
        for (AiStandardLibraryEmbedding embedding : staleEmbeddings) {
            rebuildOne(embedding);
        }
    }

    private void ensureEnabledItemsHaveEmbeddingRows(String model) {
        standardLibraryService.enabledSearchLocationItems().forEach(item -> {
            String hash = contentHash(item);
            AiStandardLibraryEmbedding embedding = embeddingRepository.findByItemAndEmbeddingModel(item, model)
                    .orElseGet(() -> AiStandardLibraryEmbedding.builder()
                            .item(item)
                            .embeddingModel(model)
                            .status("STALE")
                            .build());
            if (embedding.getId() == null || !hash.equals(embedding.getContentHash())) {
                embedding.setContentHash(hash);
                embedding.setStatus("STALE");
                embedding.setFailureReason("标准库条目内容变化，等待重建 embedding。");
                embeddingRepository.save(embedding);
            }
        });
    }

    private void rebuildOne(AiStandardLibraryEmbedding embedding) {
        AiStandardLibraryItem item = embedding.getItem();
        if (item == null) {
            embedding.setStatus("FAILED");
            embedding.setFailureReason("标准库条目不存在。");
            embeddingRepository.save(embedding);
            return;
        }
        EmbeddingClient.EmbeddingResponse response = embeddingClient.embed(searchText(item));
        if ("READY".equals(response.status())) {
            embedding.setVectorJson(vectorJson(response.vector()));
            embedding.setContentHash(contentHash(item));
            embedding.setStatus("READY");
            embedding.setFailureReason("");
        } else {
            embedding.setStatus("FAILED");
            embedding.setFailureReason(response.failureReason());
            log.warn("Standard library embedding rebuild failed. itemCode={}, reason={}",
                    item.getCode(), response.failureReason());
        }
        embeddingRepository.save(embedding);
    }

    private String vectorJson(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector == null ? List.of() : vector);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private String normalizeModel() {
        String model = embeddingProperties.getModel();
        return model == null || model.isBlank() ? "default-embedding-model" : model.trim();
    }

    private String contentHash(AiStandardLibraryItem item) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(searchText(item).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private String searchText(AiStandardLibraryItem item) {
        if (item == null) {
            return "";
        }
        return String.join("\n",
                safe(item.getLayer() == null ? "" : item.getLayer().name()),
                safe(item.getCode()),
                safe(item.getCategory()),
                safe(item.getName()),
                safe(item.getDescription()),
                safe(item.getStudentExplanation()),
                safe(item.getTeacherExplanation()),
                safe(item.getSkillUnitCode()),
                safe(item.getMistakeType()),
                safe(item.getCommonMisconception()),
                safe(item.getEvidenceSignals()),
                safe(item.getCommonCodePatterns()),
                safe(item.getJudgeSignals()),
                safe(item.getRequiredEvidence()),
                safe(item.getWhenToUse()),
                safe(item.getStudentBenefit()),
                safe(item.getAbilityPoint()),
                safe(item.getApplicableLanguages()),
                safe(item.getRelatedItems()),
                safe(item.getKnowledgeNodeCodes()),
                safe(item.getPrerequisiteKnowledgeCodes())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
