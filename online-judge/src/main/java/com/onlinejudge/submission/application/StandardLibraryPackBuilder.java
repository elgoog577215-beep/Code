package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class StandardLibraryPackBuilder {

    private final DiagnosisTaxonomy diagnosisTaxonomy;

    public StandardLibraryPackBuilder(DiagnosisTaxonomy diagnosisTaxonomy) {
        this.diagnosisTaxonomy = diagnosisTaxonomy;
    }

    public StandardLibraryPack build(ModelDiagnosisBrief brief,
                                     RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        Set<String> issueTagIds = new LinkedHashSet<>();
        Set<String> fineTagIds = new LinkedHashSet<>();

        if (brief != null && brief.getAllowedIssueTags() != null) {
            issueTagIds.addAll(brief.getAllowedIssueTags());
        }
        if (brief != null && brief.getAllowedFineGrainedTags() != null) {
            fineTagIds.addAll(brief.getAllowedFineGrainedTags());
        }
        if (ruleSignals != null && ruleSignals.getCandidateIssueTags() != null) {
            issueTagIds.addAll(ruleSignals.getCandidateIssueTags());
        }
        if (ruleSignals != null && ruleSignals.getCandidateFineGrainedTags() != null) {
            fineTagIds.addAll(ruleSignals.getCandidateFineGrainedTags());
        }

        issueTagIds.add("NEEDS_MORE_EVIDENCE");

        List<StandardLibraryPack.TagOption> issueTags = issueTagIds.stream()
                .map(diagnosisTaxonomy::get)
                .filter(tag -> tag != null && !tag.isFineGrained())
                .map(this::toTagOption)
                .toList();
        List<StandardLibraryPack.TagOption> fineTags = fineTagIds.stream()
                .map(diagnosisTaxonomy::get)
                .filter(tag -> tag != null && tag.isFineGrained())
                .map(this::toTagOption)
                .toList();
        List<StandardLibraryPack.TeachingActionOption> teachingActions = new LinkedHashSet<>(
                issueTags.stream().map(StandardLibraryPack.TagOption::getTeachingAction).filter(this::present).toList()
        ).stream()
                .map(this::toTeachingActionOption)
                .toList();

        return StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .issueTags(issueTags)
                .fineGrainedTags(fineTags)
                .teachingActions(teachingActions)
                .safetyRules(List.of(
                        "Do not provide complete code.",
                        "Do not provide direct final answers.",
                        "Do not guess or reveal hidden test data.",
                        "Prefer evidence-grounded hints and one verifiable next action."
                ))
                .uncertaintyOptions(List.of(
                        "NEEDS_MORE_EVIDENCE",
                        "LOW_CONFIDENCE",
                        "HIDDEN_TEST_DATA_UNAVAILABLE"
                ))
                .build();
    }

    private StandardLibraryPack.TagOption toTagOption(DiagnosisTaxonomy.DiagnosisTag tag) {
        return StandardLibraryPack.TagOption.builder()
                .id(tag.getId())
                .label(tag.getLabel())
                .studentExplanation(tag.getStudentExplanation())
                .teacherExplanation(tag.getTeacherExplanation())
                .abilityPoint(tag.getAbilityPoint())
                .parentTag(tag.getParentTag())
                .teachingAction(tag.getTeachingAction())
                .build();
    }

    private StandardLibraryPack.TeachingActionOption toTeachingActionOption(String action) {
        return StandardLibraryPack.TeachingActionOption.builder()
                .id(action)
                .label(action)
                .whenToUse(resolveWhenToUse(action))
                .studentTaskTemplate(resolveStudentTaskTemplate(action))
                .build();
    }

    private String resolveWhenToUse(String action) {
        return switch (action) {
            case "ASK_MIN_CASE" -> "Use when boundary or special cases are likely.";
            case "TRACE_VARIABLES" -> "Use when loop variables, indexes, or counters may be wrong.";
            case "COMPARE_OUTPUT" -> "Use when visible output differs from expected output.";
            case "COUNT_COMPLEXITY" -> "Use when time or scale is the key evidence.";
            case "DEFINE_STATE" -> "Use when state meaning or transition is unclear.";
            case "BUILD_COUNTEREXAMPLE" -> "Use when generality or greedy assumptions need testing.";
            case "COMPARE_SUBMISSIONS" -> "Use when a recent fix may have introduced regression.";
            case "CHECK_RUNTIME_GUARDS" -> "Use when runtime stability is the current failure.";
            case "COLLECT_EVIDENCE" -> "Use when available evidence is insufficient.";
            default -> "Use when this action best matches the selected diagnosis tag.";
        };
    }

    private String resolveStudentTaskTemplate(String action) {
        return switch (action) {
            case "ASK_MIN_CASE" -> "Construct one minimal input and manually check the expected behavior.";
            case "TRACE_VARIABLES" -> "Trace the key variables for the first and last loop iteration.";
            case "COMPARE_OUTPUT" -> "Compare actual and expected output character by character.";
            case "COUNT_COMPLEXITY" -> "Estimate the number of core operations at maximum input size.";
            case "DEFINE_STATE" -> "Write what each state means before and after one transition.";
            case "BUILD_COUNTEREXAMPLE" -> "Try to build a small case where the current assumption fails.";
            case "COMPARE_SUBMISSIONS" -> "Compare the last two submissions and identify the changed behavior.";
            case "CHECK_RUNTIME_GUARDS" -> "Find the smallest input that can trigger the runtime risk.";
            case "COLLECT_EVIDENCE" -> "Run or describe one more targeted example before changing code.";
            default -> "Perform one small check that can confirm or reject this diagnosis.";
        };
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
