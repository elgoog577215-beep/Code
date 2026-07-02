package com.onlinejudge.learning.standardlibrary.application;

import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryGrowthCandidate;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryGrowthCandidateStatus;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryGrowthCandidateRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryItemRepository;
import com.onlinejudge.submission.application.AdviceGenerationOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiStandardLibraryGrowthAgentService {

    private static final double MIN_CONFIDENCE = 0.6;

    private final AiStandardLibraryGrowthCandidateRepository candidateRepository;
    private final AiStandardLibraryItemRepository itemRepository;
    private final AiStandardLibraryService standardLibraryService;
    private final AiStandardLibraryGrowthProperties properties;

    @Transactional
    public AiStandardLibraryGrowthCandidate propose(StandardLibraryGrowthProposal proposal) {
        String code = normalizeCode(proposal == null ? "" : proposal.getSuggestedCode());
        AiStandardLibraryLayer layer = proposal == null ? null : proposal.getLayer();
        Optional<AiStandardLibraryGrowthCandidate> duplicateCandidate = layer == null || code.isBlank()
                ? Optional.empty()
                : candidateRepository.findByLayerAndSuggestedCode(layer, code);
        if (duplicateCandidate.isPresent()) {
            return aggregateDuplicate(duplicateCandidate.get(), proposal);
        }
        List<String> precheckErrors = precheck(proposal, layer, code);
        AiStandardLibraryGrowthCandidateStatus status = precheckErrors.isEmpty()
                ? AiStandardLibraryGrowthCandidateStatus.PROPOSED
                : AiStandardLibraryGrowthCandidateStatus.BLOCKED;

        AiStandardLibraryGrowthCandidate candidate = AiStandardLibraryGrowthCandidate.builder()
                .layer(layer == null ? AiStandardLibraryLayer.MISTAKE_POINT : layer)
                .suggestedCode(code.isBlank() ? "UNSPECIFIED_GROWTH_CANDIDATE" : code)
                .suggestedName(requiredText(proposal == null ? "" : proposal.getSuggestedName(), "未命名标准库候选"))
                .suggestedPath(joinPath(proposal == null ? null : proposal.getSuggestedPath()))
                .sourceProblemId(proposal == null ? null : proposal.getSourceProblemId())
                .sourceSubmissionId(proposal == null ? null : proposal.getSourceSubmissionId())
                .similarExistingItems(joinLines(proposal == null ? null : proposal.getSimilarExistingItemCodes()))
                .changeReason(requiredText(proposal == null ? "" : proposal.getChangeReason(), "缺少变更理由"))
                .evidenceRefs(joinLines(proposal == null ? null : proposal.getEvidenceRefs()))
                .confidence(proposal == null ? null : proposal.getConfidence())
                .status(status)
                .precheckMessage(String.join("；", precheckErrors))
                .occurrenceCount(1)
                .lastObservedAt(LocalDateTime.now())
                .beforeSnapshot(beforeSnapshot(layer, code))
                .diffSummary(diffSummary(proposal, status))
                .rollbackInfo(rollbackInfo(status))
                .build();
        return candidateRepository.save(candidate);
    }

    @Transactional
    public List<AiStandardLibraryGrowthCandidate> proposeFromDiagnosisOutput(AdviceGenerationOutput output) {
        if (output == null || output.getLibraryGrowth() == null || output.getLibraryGrowth().getCandidates() == null) {
            return List.of();
        }
        if (!diagnosisAllowsGrowth(output)) {
            return List.of();
        }
        return output.getLibraryGrowth().getCandidates().stream()
                .filter(candidate -> candidate != null)
                .map(candidate -> propose(StandardLibraryGrowthProposal.builder()
                        .suggestedCode(suggestedCode(candidate))
                        .suggestedName(candidate.getName())
                        .layer(AiStandardLibraryLayer.MISTAKE_POINT)
                        .suggestedPath(candidate.getSuggestedPath())
                        .sourceProblemId(candidate.getSourceProblemId())
                        .sourceSubmissionId(candidate.getSourceSubmissionId())
                        .similarExistingItemCodes(candidate.getSimilarExistingItems())
                        .changeReason(candidate.getReason())
                        .confidence(candidate.getConfidence())
                        .build()))
                .toList();
    }

    private boolean diagnosisAllowsGrowth(AdviceGenerationOutput output) {
        AdviceGenerationOutput.DiagnosisDecision decision = output.getDiagnosisDecision();
        if (decision == null || decision.getLibraryFit() == null) {
            return true;
        }
        String fit = decision.getLibraryFit().trim().toUpperCase(Locale.ROOT);
        if ("PARTIAL".equals(fit) || "MISS".equals(fit)) {
            return true;
        }
        return decision.getAnchors() != null && decision.getAnchors().stream()
                .anyMatch(anchor -> anchor != null && "OUT_OF_LIBRARY".equalsIgnoreCase(anchor.getType()));
    }

    @Transactional(readOnly = true)
    public List<AiStandardLibraryGrowthCandidate> listCandidates() {
        return candidateRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public AiStandardLibraryGrowthCandidate getCandidate(Long id) {
        return findCandidate(id);
    }

    @Transactional
    public AiStandardLibraryGrowthCandidate updateCandidate(Long id, StandardLibraryGrowthProposal proposal) {
        AiStandardLibraryGrowthCandidate candidate = findCandidate(id);
        String code = normalizeCode(proposal == null ? candidate.getSuggestedCode() : proposal.getSuggestedCode());
        AiStandardLibraryLayer layer = proposal == null ? candidate.getLayer() : proposal.getLayer();
        List<String> precheckErrors = precheckForCandidate(proposal, layer, code, id);
        candidate.setLayer(layer == null ? candidate.getLayer() : layer);
        candidate.setSuggestedCode(code.isBlank() ? candidate.getSuggestedCode() : code);
        candidate.setSuggestedName(requiredText(proposal == null ? candidate.getSuggestedName() : proposal.getSuggestedName(), candidate.getSuggestedName()));
        candidate.setSuggestedPath(joinPath(proposal == null ? splitPath(candidate.getSuggestedPath()) : proposal.getSuggestedPath()));
        candidate.setSourceProblemId(proposal == null ? candidate.getSourceProblemId() : proposal.getSourceProblemId());
        candidate.setSourceSubmissionId(proposal == null ? candidate.getSourceSubmissionId() : proposal.getSourceSubmissionId());
        candidate.setSimilarExistingItems(joinLines(proposal == null ? lines(candidate.getSimilarExistingItems()) : proposal.getSimilarExistingItemCodes()));
        candidate.setChangeReason(requiredText(proposal == null ? candidate.getChangeReason() : proposal.getChangeReason(), candidate.getChangeReason()));
        candidate.setEvidenceRefs(joinLines(proposal == null ? lines(candidate.getEvidenceRefs()) : proposal.getEvidenceRefs()));
        candidate.setConfidence(proposal == null ? candidate.getConfidence() : proposal.getConfidence());
        candidate.setStatus(precheckErrors.isEmpty()
                ? AiStandardLibraryGrowthCandidateStatus.PROPOSED
                : AiStandardLibraryGrowthCandidateStatus.BLOCKED);
        candidate.setPrecheckMessage(String.join("；", precheckErrors));
        candidate.setBeforeSnapshot(beforeSnapshot(candidate.getLayer(), candidate.getSuggestedCode()));
        candidate.setDiffSummary(diffSummary(candidate, candidate.getStatus()));
        candidate.setRollbackInfo(rollbackInfo(candidate.getStatus()));
        return candidateRepository.save(candidate);
    }

    @Transactional
    public AiStandardLibraryGrowthCandidate ignore(Long id, String teacherNote) {
        AiStandardLibraryGrowthCandidate candidate = findCandidate(id);
        candidate.setStatus(AiStandardLibraryGrowthCandidateStatus.IGNORED);
        candidate.setTeacherNote(requiredText(teacherNote, "教师已忽略该候选。"));
        candidate.setPrecheckMessage(requiredText(teacherNote, "教师已忽略该候选。"));
        candidate.setRollbackInfo("未写入正式标准库；恢复时将状态改回 PROPOSED 后重新处理。");
        candidate.setDiffSummary(candidate.getDiffSummary() + "\nignoredReason=" + candidate.getPrecheckMessage());
        return candidateRepository.save(candidate);
    }

    @Transactional
    public AiStandardLibraryGrowthCandidate reject(Long id, String teacherNote) {
        AiStandardLibraryGrowthCandidate candidate = findCandidate(id);
        candidate.setStatus(AiStandardLibraryGrowthCandidateStatus.REJECTED);
        candidate.setTeacherNote(requiredText(teacherNote, "教师拒绝该候选。"));
        candidate.setPrecheckMessage(candidate.getTeacherNote());
        candidate.setRollbackInfo("未写入正式标准库；同类低质候选会优先聚合到本记录，避免反复推荐。");
        candidate.setDiffSummary(candidate.getDiffSummary() + "\nrejectedReason=" + candidate.getPrecheckMessage());
        return candidateRepository.save(candidate);
    }

    @Transactional
    public AiStandardLibraryGrowthCandidate approve(Long id, StandardLibraryGrowthProposal overrideProposal) {
        AiStandardLibraryGrowthCandidate candidate = mergeToFormalLibrary(id, overrideProposal);
        if (candidate.getStatus() == AiStandardLibraryGrowthCandidateStatus.MERGED) {
            candidate.setStatus(AiStandardLibraryGrowthCandidateStatus.TEACHER_APPROVED);
            candidate.setPrecheckMessage(candidate.getPrecheckMessage() + "；教师已批准。");
            candidate.setRollbackInfo(candidate.getRollbackInfo() + " 本状态表示由教师审核批准入库。");
            return candidateRepository.save(candidate);
        }
        return candidate;
    }

    @Transactional
    public AiStandardLibraryGrowthCandidate autoMergeToFormalLibrary(Long id, StandardLibraryGrowthProposal overrideProposal) {
        AiStandardLibraryGrowthCandidate candidate = findCandidate(id);
        if (!properties.isEnabled()) {
            return markNeedsReview(candidate, "标准库成长 Agent 已关闭。");
        }
        if (!properties.isAutoMergeEnabled()) {
            return markNeedsReview(candidate, "自动入库未开启；候选保留给教师确认。");
        }
        if (candidate.getConfidence() == null || candidate.getConfidence() < properties.getAutoMergeMinConfidence()) {
            return markNeedsReview(candidate, "自动入库置信度不足。");
        }
        return mergeToFormalLibrary(id, overrideProposal);
    }

    @Transactional
    public AiStandardLibraryGrowthCandidate mergeToFormalLibrary(Long id, StandardLibraryGrowthProposal overrideProposal) {
        AiStandardLibraryGrowthCandidate candidate = findCandidate(id);
        if (candidate.getStatus() == AiStandardLibraryGrowthCandidateStatus.MERGED
                || candidate.getStatus() == AiStandardLibraryGrowthCandidateStatus.TEACHER_APPROVED) {
            return candidate;
        }
        StandardLibraryGrowthProposal proposal = overrideProposal == null ? proposalFrom(candidate) : overrideProposal;
        List<String> precheckErrors = precheckForCandidate(
                proposal,
                proposal.getLayer(),
                normalizeCode(proposal.getSuggestedCode()),
                candidate.getId()
        );
        if (!precheckErrors.isEmpty()) {
            candidate.setStatus(AiStandardLibraryGrowthCandidateStatus.NEEDS_REVIEW);
            candidate.setPrecheckMessage(String.join("；", precheckErrors));
            candidate.setRollbackInfo("未写入正式标准库；请编辑候选或手动合并相似条目。");
            return candidateRepository.save(candidate);
        }

        candidate.setBeforeSnapshot(beforeSnapshot(proposal.getLayer(), normalizeCode(proposal.getSuggestedCode())));
        var created = standardLibraryService.create(toItemRequest(proposal));
        candidate.setStatus(AiStandardLibraryGrowthCandidateStatus.MERGED);
        candidate.setPrecheckMessage("已写入正式标准库: " + created.getLayer() + "/" + created.getCode());
        candidate.setDiffSummary(diffSummary(candidate, AiStandardLibraryGrowthCandidateStatus.MERGED)
                + "\nformalItemId=" + created.getId());
        candidate.setRollbackInfo("已写入正式标准库；如需回滚，请在 AI 标准库中停用或删除条目 "
                + created.getLayer() + "/" + created.getCode()
                + "，并保留本候选审计记录。");
        return candidateRepository.save(candidate);
    }

    private List<String> precheck(StandardLibraryGrowthProposal proposal, AiStandardLibraryLayer layer, String code) {
        return precheckForCandidate(proposal, layer, code, null);
    }

    private AiStandardLibraryGrowthCandidate aggregateDuplicate(AiStandardLibraryGrowthCandidate existing,
                                                                StandardLibraryGrowthProposal proposal) {
        int count = existing.getOccurrenceCount() == null ? 1 : existing.getOccurrenceCount();
        existing.setOccurrenceCount(count + 1);
        existing.setLastObservedAt(LocalDateTime.now());
        existing.setEvidenceRefs(joinLines(mergeLines(lines(existing.getEvidenceRefs()),
                proposal == null ? List.of() : proposal.getEvidenceRefs())));
        existing.setSimilarExistingItems(joinLines(mergeLines(lines(existing.getSimilarExistingItems()),
                proposal == null ? List.of() : proposal.getSimilarExistingItemCodes())));
        String reason = requiredText(proposal == null ? "" : proposal.getChangeReason(), "");
        String existingReason = requiredText(existing.getChangeReason(), "");
        if (!reason.isBlank() && !existingReason.contains(reason)) {
            existing.setChangeReason(existingReason.isBlank() ? reason : existingReason + "\n再次出现：" + reason);
        }
        if (List.of(
                AiStandardLibraryGrowthCandidateStatus.PROPOSED,
                AiStandardLibraryGrowthCandidateStatus.NEEDS_REVIEW,
                AiStandardLibraryGrowthCandidateStatus.BLOCKED,
                AiStandardLibraryGrowthCandidateStatus.MERGED_SIMILAR
        ).contains(existing.getStatus())) {
            existing.setStatus(AiStandardLibraryGrowthCandidateStatus.MERGED_SIMILAR);
        }
        existing.setPrecheckMessage("相似库外发现已聚合，出现次数=" + existing.getOccurrenceCount());
        existing.setDiffSummary(diffSummary(existing, existing.getStatus())
                + "\noccurrenceCount=" + existing.getOccurrenceCount());
        existing.setRollbackInfo("未自动写入正式标准库；该记录用于教师按频次审核。");
        return candidateRepository.save(existing);
    }

    private List<String> precheckForCandidate(StandardLibraryGrowthProposal proposal,
                                              AiStandardLibraryLayer layer,
                                              String code,
                                              Long currentCandidateId) {
        List<String> errors = new ArrayList<>();
        if (layer == null) {
            errors.add("层级缺失");
        }
        if (code.isBlank()) {
            errors.add("建议 ID 缺失");
        }
        if (proposal == null || proposal.getSourceProblemId() == null || proposal.getSourceSubmissionId() == null) {
            errors.add("来源题目或提交缺失");
        }
        if (proposal == null || proposal.getSuggestedPath() == null || proposal.getSuggestedPath().isEmpty()) {
            errors.add("归属路径缺失");
        }
        if (proposal == null || proposal.getConfidence() == null || proposal.getConfidence() < MIN_CONFIDENCE) {
            errors.add("置信度不足");
        }
        if (layer != null && !code.isBlank() && itemRepository.existsByLayerAndCode(layer, code)) {
            errors.add("正式标准库已存在: " + layer + "/" + code);
        }
        Optional<AiStandardLibraryGrowthCandidate> duplicateCandidate = layer == null || code.isBlank()
                ? Optional.empty()
                : candidateRepository.findByLayerAndSuggestedCode(layer, code);
        if (duplicateCandidate.isPresent()
                && (currentCandidateId == null || !duplicateCandidate.get().getId().equals(currentCandidateId))) {
            errors.add("候选池已存在: " + layer + "/" + code);
        }
        return errors;
    }

    private String beforeSnapshot(AiStandardLibraryLayer layer, String code) {
        if (layer == null || code == null || code.isBlank()) {
            return "NO_EXISTING_ITEM";
        }
        Optional<AiStandardLibraryItem> item = itemRepository.findByLayerAndCode(layer, code);
        if (item.isEmpty()) {
            return "NO_EXISTING_ITEM";
        }
        AiStandardLibraryItem existing = item.get();
        return "layer=" + existing.getLayer()
                + "\ncode=" + existing.getCode()
                + "\nname=" + existing.getName()
                + "\ncategory=" + existing.getCategory()
                + "\ndescription=" + existing.getDescription();
    }

    private String diffSummary(StandardLibraryGrowthProposal proposal, AiStandardLibraryGrowthCandidateStatus status) {
        return "status=" + status
                + "\nname=" + requiredText(proposal == null ? "" : proposal.getSuggestedName(), "未命名标准库候选")
                + "\nreason=" + requiredText(proposal == null ? "" : proposal.getChangeReason(), "缺少变更理由");
    }

    private String diffSummary(AiStandardLibraryGrowthCandidate candidate, AiStandardLibraryGrowthCandidateStatus status) {
        return "status=" + status
                + "\nname=" + requiredText(candidate == null ? "" : candidate.getSuggestedName(), "未命名标准库候选")
                + "\ncode=" + requiredText(candidate == null ? "" : candidate.getSuggestedCode(), "UNKNOWN")
                + "\nreason=" + requiredText(candidate == null ? "" : candidate.getChangeReason(), "缺少变更理由");
    }

    private String rollbackInfo(AiStandardLibraryGrowthCandidateStatus status) {
        if (status == AiStandardLibraryGrowthCandidateStatus.PROPOSED) {
            return "候选尚未写入正式标准库；删除或忽略候选即可回滚。";
        }
        return "未写入正式标准库；修正预检问题后重新生成候选。";
    }

    private AiStandardLibraryGrowthCandidate markNeedsReview(AiStandardLibraryGrowthCandidate candidate, String message) {
        candidate.setStatus(AiStandardLibraryGrowthCandidateStatus.NEEDS_REVIEW);
        candidate.setPrecheckMessage(message);
        candidate.setRollbackInfo("未写入正式标准库；候选保留在待处理列表中。");
        return candidateRepository.save(candidate);
    }

    private AiStandardLibraryGrowthCandidate findCandidate(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标准库成长候选不存在: " + id));
    }

    private StandardLibraryGrowthProposal proposalFrom(AiStandardLibraryGrowthCandidate candidate) {
        return StandardLibraryGrowthProposal.builder()
                .suggestedCode(candidate.getSuggestedCode())
                .suggestedName(candidate.getSuggestedName())
                .layer(candidate.getLayer())
                .suggestedPath(splitPath(candidate.getSuggestedPath()))
                .sourceProblemId(candidate.getSourceProblemId())
                .sourceSubmissionId(candidate.getSourceSubmissionId())
                .similarExistingItemCodes(lines(candidate.getSimilarExistingItems()))
                .changeReason(candidate.getChangeReason())
                .evidenceRefs(lines(candidate.getEvidenceRefs()))
                .confidence(candidate.getConfidence())
                .build();
    }

    private com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryItemRequest toItemRequest(StandardLibraryGrowthProposal proposal) {
        String code = normalizeCode(proposal.getSuggestedCode());
        String path = joinPath(proposal.getSuggestedPath());
        List<String> evidence = proposal.getEvidenceRefs() == null ? List.of() : proposal.getEvidenceRefs();
        com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryItemRequest request =
                new com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryItemRequest();
        request.setLayer(proposal.getLayer().name());
        request.setCode(code);
        request.setCategory(categoryFromPath(path));
        request.setName(requiredText(proposal.getSuggestedName(), code));
        request.setDescription(requiredText(proposal.getChangeReason(), "由 AI 诊断发现并经门禁写入的细颗粒标准库条目。"));
        request.setStudentExplanation("这个条目来自真实提交诊断，用于帮助 AI 以后更细地定位同类问题。");
        request.setTeacherExplanation(proposal.getChangeReason());
        request.setSkillUnitCode(skillUnitFromSimilarItems(proposal.getSimilarExistingItemCodes()));
        request.setMistakeType("DEBUGGING");
        request.setCommonMisconception(requiredText(proposal.getChangeReason(), "学生可能没有把代码行为、判题结果和知识点边界对齐。"));
        request.setEvidenceSignals(evidence);
        request.setCommonCodePatterns(List.of());
        request.setJudgeSignals(evidence.stream().filter(ref -> ref.startsWith("judge:")).toList());
        request.setRequiredEvidence(evidence);
        request.setApplicableLanguages(List.of("PYTHON", "CPP17"));
        request.setRelatedItems(proposal.getSimilarExistingItemCodes());
        request.setKnowledgeNodeCodes(knowledgeNodeCodes(proposal.getSuggestedPath()));
        request.setPrerequisiteKnowledgeCodes(List.of());
        request.setSeverity("MEDIUM");
        request.setEnabled(true);
        request.setLibraryVersion("standard-library-growth-v1");
        return request;
    }

    private String categoryFromPath(String path) {
        if (path == null || path.isBlank()) {
            return "AI 扩库";
        }
        String first = path.split("\\.")[0];
        return first.isBlank() ? "AI 扩库" : first;
    }

    private String skillUnitFromSimilarItems(List<String> similarItems) {
        if (similarItems == null || similarItems.isEmpty()) {
            return "SK_AI_GROWTH_REVIEW";
        }
        return similarItems.stream()
                .map(this::normalizeText)
                .filter(value -> value.startsWith("SK_"))
                .findFirst()
                .orElse("SK_AI_GROWTH_REVIEW");
    }

    private List<String> knowledgeNodeCodes(List<String> path) {
        if (path == null || path.isEmpty()) {
            return List.of("AI_GROWTH_REVIEW");
        }
        return path.stream()
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]+", "_"))
                .toList();
    }

    private List<String> splitPath(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\.")).stream()
                .map(this::normalizeText)
                .filter(path -> !path.isBlank())
                .toList();
    }

    private List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(this::normalizeText)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private List<String> mergeLines(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            second.stream().map(this::normalizeText).filter(value -> !value.isBlank()).forEach(merged::add);
        }
        return merged.stream().toList();
    }

    private String joinPath(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(".", values.stream()
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .toList());
    }

    private String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values.stream()
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .toList());
    }

    private String normalizeCode(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String suggestedCode(AdviceGenerationOutput.LibraryGrowthCandidate candidate) {
        String pathCode = candidate.getSuggestedPath() == null || candidate.getSuggestedPath().isEmpty()
                ? ""
                : String.join("_", candidate.getSuggestedPath());
        String normalized = normalizeCode(pathCode);
        if (normalized.isBlank()) {
            normalized = normalizeCode(candidate.getName());
        }
        if (normalized.startsWith("MP_")) {
            return normalized;
        }
        return "MP_" + normalized;
    }

    private String requiredText(String value, String fallback) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
