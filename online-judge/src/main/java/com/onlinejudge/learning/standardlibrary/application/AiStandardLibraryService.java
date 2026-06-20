package com.onlinejudge.learning.standardlibrary.application;

import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryItemRequest;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryItemResponse;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryEmbeddingRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryItemRepository;
import com.onlinejudge.submission.application.StandardLibraryPack;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiStandardLibraryService {

    private final AiStandardLibraryItemRepository repository;
    private final AiStandardLibraryEmbeddingRepository embeddingRepository;

    @Transactional(readOnly = true)
    public List<AiStandardLibraryItemResponse> list(String layer, String category, String enabled, String query) {
        AiStandardLibraryLayer layerFilter = parseLayerOrNull(layer);
        Boolean enabledFilter = parseBooleanOrNull(enabled);
        String categoryFilter = normalizeText(category).toLowerCase(Locale.ROOT);
        String queryFilter = normalizeText(query).toLowerCase(Locale.ROOT);
        return repository.findAllByOrderByLayerAscCategoryAscCodeAsc().stream()
                .filter(item -> layerFilter == null || item.getLayer() == layerFilter)
                .filter(item -> enabledFilter == null || item.isEnabled() == enabledFilter)
                .filter(item -> categoryFilter.isBlank() || item.getCategory().toLowerCase(Locale.ROOT).contains(categoryFilter))
                .filter(item -> matchesQuery(item, queryFilter))
                .map(AiStandardLibraryItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiStandardLibraryItemResponse get(Long id) {
        return AiStandardLibraryItemResponse.from(find(id));
    }

    @Transactional
    public AiStandardLibraryItemResponse create(AiStandardLibraryItemRequest request) {
        AiStandardLibraryLayer layer = parseLayer(request.getLayer());
        String code = normalizeCode(request.getCode());
        if (repository.existsByLayerAndCode(layer, code)) {
            throw new IllegalArgumentException("标准库条目已存在: " + layer + "/" + code);
        }
        AiStandardLibraryItem item = new AiStandardLibraryItem();
        item.setLayer(layer);
        item.setCode(code);
        apply(item, request);
        AiStandardLibraryItem saved = repository.save(item);
        markEmbeddingStale(saved);
        return AiStandardLibraryItemResponse.from(saved);
    }

    @Transactional
    public AiStandardLibraryItemResponse update(Long id, AiStandardLibraryItemRequest request) {
        AiStandardLibraryItem item = find(id);
        AiStandardLibraryLayer requestedLayer = parseLayer(request.getLayer());
        String requestedCode = normalizeCode(request.getCode());
        if (item.getLayer() != requestedLayer || !item.getCode().equals(requestedCode)) {
            Optional<AiStandardLibraryItem> duplicate = repository.findByLayerAndCode(requestedLayer, requestedCode);
            if (duplicate.isPresent() && !duplicate.get().getId().equals(item.getId())) {
                throw new IllegalArgumentException("标准库条目已存在: " + requestedLayer + "/" + requestedCode);
            }
            item.setLayer(requestedLayer);
            item.setCode(requestedCode);
        }
        apply(item, request);
        AiStandardLibraryItem saved = repository.save(item);
        markEmbeddingStale(saved);
        return AiStandardLibraryItemResponse.from(saved);
    }

    @Transactional
    public AiStandardLibraryItemResponse setEnabled(Long id, boolean enabled) {
        AiStandardLibraryItem item = find(id);
        item.setEnabled(enabled);
        AiStandardLibraryItem saved = repository.save(item);
        markEmbeddingStale(saved);
        return AiStandardLibraryItemResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<StandardLibraryPack.BasicCauseOption> enabledBasicCauses() {
        return repository.findByEnabledTrueOrderByLayerAscCategoryAscCodeAsc().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT
                        || item.getLayer() == AiStandardLibraryLayer.BASIC_CAUSE)
                .sorted(Comparator.comparing(AiStandardLibraryItem::getCategory).thenComparing(AiStandardLibraryItem::getCode))
                .map(this::toBasicCause)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StandardLibraryPack.ImprovementPointOption> enabledImprovementPoints() {
        return repository.findByEnabledTrueOrderByLayerAscCategoryAscCodeAsc().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.IMPROVEMENT_POINT)
                .sorted(Comparator.comparing(AiStandardLibraryItem::getCategory).thenComparing(AiStandardLibraryItem::getCode))
                .map(this::toImprovementPoint)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasEnabledKnowledge() {
        return repository.findByEnabledTrueOrderByLayerAscCategoryAscCodeAsc().stream().findAny().isPresent();
    }

    @Transactional(readOnly = true)
    public List<AiStandardLibraryItem> enabledSearchLocationItems() {
        return repository.findByEnabledTrueOrderByLayerAscCategoryAscCodeAsc().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT
                        || item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT
                        || item.getLayer() == AiStandardLibraryLayer.IMPROVEMENT_POINT
                        || item.getLayer() == AiStandardLibraryLayer.BASIC_CAUSE)
                .toList();
    }

    @Transactional
    public void markEmbeddingStale(AiStandardLibraryItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        embeddingRepository.findAll().stream()
                .filter(embedding -> embedding.getItem() != null && item.getId().equals(embedding.getItem().getId()))
                .forEach(embedding -> {
                    embedding.setStatus("STALE");
                    embedding.setFailureReason("标准库条目已更新，等待重建 embedding。");
                    embeddingRepository.save(embedding);
                });
    }

    StandardLibraryPack.BasicCauseOption toBasicCause(AiStandardLibraryItem item) {
        if (item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT) {
            return StandardLibraryPack.BasicCauseOption.builder()
                    .id(item.getCode())
                    .category(item.getCategory())
                    .name(item.getName())
                    .description(item.getDescription())
                    .studentExplanation(item.getCommonMisconception())
                    .teacherExplanation(item.getTeacherExplanation())
                    .evidenceSignals(List.of())
                    .commonCodePatterns(List.of())
                    .judgeSignals(List.of())
                    .hintL1("")
                    .hintL2("")
                    .hintL3("")
                    .abilityPoint(item.getSkillUnitCode())
                    .severity(item.getSeverity())
                    .applicableLanguages(lines(item.getApplicableLanguages()))
                    .relatedFineTags(lines(item.getKnowledgeNodeCodes()))
                    .teachingAction("")
                    .build();
        }
        return StandardLibraryPack.BasicCauseOption.builder()
                .id(item.getCode())
                .category(item.getCategory())
                .name(item.getName())
                .description(item.getDescription())
                .studentExplanation(item.getStudentExplanation())
                .teacherExplanation(item.getTeacherExplanation())
                .evidenceSignals(lines(item.getEvidenceSignals()))
                .commonCodePatterns(lines(item.getCommonCodePatterns()))
                .judgeSignals(lines(item.getJudgeSignals()))
                .hintL1(item.getHintL1())
                .hintL2(item.getHintL2())
                .hintL3(item.getHintL3())
                .abilityPoint(item.getAbilityPoint())
                .severity(item.getSeverity())
                .applicableLanguages(lines(item.getApplicableLanguages()))
                .relatedFineTags(lines(item.getRelatedItems()))
                .teachingAction(item.getTeachingAction())
                .build();
    }

    StandardLibraryPack.ImprovementPointOption toImprovementPoint(AiStandardLibraryItem item) {
        return StandardLibraryPack.ImprovementPointOption.builder()
                .id(item.getCode())
                .category(item.getCategory())
                .name(item.getName())
                .description(item.getDescription())
                .whenToUse(item.getWhenToUse())
                .studentBenefit(item.getStudentBenefit())
                .teacherExplanation(item.getTeacherExplanation())
                .requiredEvidence(lines(item.getRequiredEvidence()))
                .hintL1(item.getHintL1())
                .hintL2(item.getHintL2())
                .hintL3(item.getHintL3())
                .abilityPoint(item.getAbilityPoint())
                .relatedBasicCauses(lines(item.getRelatedItems()))
                .build();
    }

    private AiStandardLibraryItem find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标准库条目不存在: " + id));
    }

    private void apply(AiStandardLibraryItem item, AiStandardLibraryItemRequest request) {
        item.setCategory(required(request.getCategory(), "分类不能为空"));
        item.setName(required(request.getName(), "名称不能为空"));
        item.setDescription(normalizeText(request.getDescription()));
        item.setStudentExplanation(normalizeText(request.getStudentExplanation()));
        item.setTeacherExplanation(normalizeText(request.getTeacherExplanation()));
        item.setSkillUnitCode(normalizeCodeOrBlank(request.getSkillUnitCode()));
        item.setMistakeType(normalizeText(request.getMistakeType()));
        item.setCommonMisconception(normalizeText(request.getCommonMisconception()));
        item.setEvidenceSignals(join(request.getEvidenceSignals()));
        item.setCommonCodePatterns(join(request.getCommonCodePatterns()));
        item.setJudgeSignals(join(request.getJudgeSignals()));
        item.setRequiredEvidence(join(request.getRequiredEvidence()));
        item.setWhenToUse(normalizeText(request.getWhenToUse()));
        item.setStudentBenefit(normalizeText(request.getStudentBenefit()));
        item.setHintL1(normalizeText(request.getHintL1()));
        item.setHintL2(normalizeText(request.getHintL2()));
        item.setHintL3(normalizeText(request.getHintL3()));
        item.setAbilityPoint(normalizeText(request.getAbilityPoint()));
        item.setSeverity(normalizeText(request.getSeverity()));
        item.setApplicableLanguages(join(request.getApplicableLanguages()));
        item.setRelatedItems(join(request.getRelatedItems()));
        item.setKnowledgeNodeCodes(join(request.getKnowledgeNodeCodes()));
        item.setPrerequisiteKnowledgeCodes(join(request.getPrerequisiteKnowledgeCodes()));
        item.setTeachingAction(normalizeText(request.getTeachingAction()));
        item.setLibraryVersion(normalizeText(request.getLibraryVersion()).isBlank()
                ? AiStandardLibrarySeedCatalog.VERSION
                : normalizeText(request.getLibraryVersion()));
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        validateKnowledgeItem(item);
    }

    private void validateKnowledgeItem(AiStandardLibraryItem item) {
        if (item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT) {
            requireText(item.getDescription(), "能力点定义不能为空");
            requireText(item.getStudentExplanation(), "能力点学习目标不能为空");
            requireLines(item.getKnowledgeNodeCodes(), "能力点必须关联至少一个知识节点");
            return;
        }
        if (item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT) {
            requireText(item.getDescription(), "易错点定义不能为空");
            requireText(item.getSkillUnitCode(), "易错点必须关联能力点");
            requireText(item.getMistakeType(), "易错点类型不能为空");
            requireText(item.getCommonMisconception(), "学生常见误解不能为空");
            requireLines(item.getKnowledgeNodeCodes(), "易错点必须关联至少一个知识节点");
        }
    }

    private void requireText(String value, String message) {
        if (normalizeText(value).isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireLines(String value, String message) {
        if (lines(value).isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean matchesQuery(AiStandardLibraryItem item, String query) {
        if (query.isBlank()) {
            return true;
        }
        return contains(item.getCode(), query)
                || contains(item.getName(), query)
                || contains(item.getCategory(), query)
                || contains(item.getDescription(), query)
                || contains(item.getTeacherExplanation(), query)
                || contains(item.getSkillUnitCode(), query)
                || contains(item.getMistakeType(), query)
                || contains(item.getCommonMisconception(), query)
                || contains(item.getKnowledgeNodeCodes(), query)
                || contains(item.getEvidenceSignals(), query)
                || contains(item.getCommonCodePatterns(), query);
    }

    private boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private AiStandardLibraryLayer parseLayer(String value) {
        try {
            return AiStandardLibraryLayer.valueOf(required(value, "层级不能为空").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("标准库层级无效: " + value);
        }
    }

    private AiStandardLibraryLayer parseLayerOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseLayer(value);
    }

    private Boolean parseBooleanOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    private String normalizeCode(String value) {
        return required(value, "条目 ID 不能为空").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCodeOrBlank(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    private String required(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values.stream()
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .toList());
    }

    private List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }
}
