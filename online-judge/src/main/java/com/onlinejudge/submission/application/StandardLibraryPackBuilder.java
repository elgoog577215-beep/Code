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
        LinkedHashSet<String> teachingActionIds = new LinkedHashSet<>();
        issueTags.stream()
                .map(StandardLibraryPack.TagOption::getTeachingAction)
                .filter(this::present)
                .forEach(teachingActionIds::add);
        fineTags.stream()
                .map(StandardLibraryPack.TagOption::getTeachingAction)
                .filter(this::present)
                .forEach(teachingActionIds::add);
        List<StandardLibraryPack.TeachingActionOption> teachingActions = teachingActionIds.stream()
                .map(this::toTeachingActionOption)
                .toList();

        return StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .issueTags(issueTags)
                .fineGrainedTags(fineTags)
                .improvementTags(buildImprovementTags())
                .teachingActions(teachingActions)
                .decisionProtocol(buildDecisionProtocol())
                .studentFeedbackRules(buildStudentFeedbackRules())
                .safetyRules(List.of(
                        "Do not provide complete code.",
                        "Do not provide direct final answers.",
                        "Do not guess or reveal hidden test data.",
                        "Do not provide replacement loop headers, transition formulas, or executable control structures.",
                        "For input-format issues, ask the student to compare required input lines with actual read operations instead of naming the exact loop to add.",
                        "Prefer evidence-grounded hints and one verifiable next action.",
                        "Student-facing improvement opportunities must not include direct code replacements or hidden test guesses."
                ))
                .uncertaintyOptions(List.of(
                        "NEEDS_MORE_EVIDENCE",
                        "LOW_CONFIDENCE",
                        "HIDDEN_TEST_DATA_UNAVAILABLE"
                ))
                .build();
    }

    private List<StandardLibraryPack.ImprovementTagOption> buildImprovementTags() {
        return List.of(
                improvement("COMPLEXITY", "复杂度提升",
                        "Use when the current or follow-up solution should be checked against maximum input scale.",
                        "帮助学生从样例正确推进到规模正确。"),
                improvement("TESTING_HABIT", "自测习惯",
                        "Use when the student would benefit from constructing non-sample, boundary, or multi-case tests.",
                        "帮助学生提前发现样例特判、漏读、多组输入和边界问题。"),
                improvement("CODE_CLARITY", "代码清晰度",
                        "Use when input, computation, output, helper functions, or debug branches can be separated more clearly.",
                        "帮助学生减少漏处理和复盘成本。"),
                improvement("BOUNDARY_AWARENESS", "边界意识",
                        "Use when minimum, maximum, empty, repeated, or single-element cases deserve follow-up checks.",
                        "帮助学生形成可迁移的边界清单。"),
                improvement("ROBUSTNESS", "鲁棒性",
                        "Use when runtime stability, guards, or extreme cases need a follow-up validation habit.",
                        "帮助学生确认修复不只覆盖眼前样例。"),
                improvement("DEBUG_CLEANUP", "调试清理",
                        "Use when temporary debug output, dead branches, or unused helpers could distract from the main flow.",
                        "帮助学生提交更干净、可读的代码。")
        );
    }

    private StandardLibraryPack.ImprovementTagOption improvement(String id,
                                                                 String label,
                                                                 String whenToUse,
                                                                 String studentBenefit) {
        return StandardLibraryPack.ImprovementTagOption.builder()
                .id(id)
                .label(label)
                .whenToUse(whenToUse)
                .studentBenefit(studentBenefit)
                .build();
    }

    private StandardLibraryPack.DecisionProtocol buildDecisionProtocol() {
        return StandardLibraryPack.DecisionProtocol.builder()
                .globalRules(List.of(
                        "Choose the most evidence-supported diagnosis, not the most common or most severe label.",
                        "Use only issueTags, fineGrainedTags, teachingActions, and evidenceRefs provided in this pack and brief.",
                        "Do not infer hidden test inputs or outputs; hidden failures only justify uncertainty.",
                        "Prefer a narrow, verifiable diagnosis when direct code or judge evidence supports it.",
                        "For medium-length code, ignore helper-method distractions and identify the behavior that changes the verdict.",
                        "Keep the student task diagnostic: ask for a trace, comparison, estimate, invariant check, or counterexample before naming a fix."
                ))
                .evidencePriorityRules(List.of(
                        "Compiler and runtime error messages outrank heuristic code-shape signals.",
                        "Visible failed case actual-vs-expected output outranks generic problem-topic signals.",
                        "Whitespace-only visible output differences outrank algorithm-topic guesses.",
                        "CandidateSignals with concrete evidenceRef outrank broad baseline summaries.",
                        "CandidateSignals with confidence >= 0.80 and concrete problem/source evidence outrank generic verdict labels.",
                        "Student learning memory is auxiliary evidence only; current submission compile/runtime/judge facts and source-code signals outrank memory signals.",
                        "Use learning memory to recognize repeated patterns, adapt teaching action, or raise teacher attention, not to override the current observable failure.",
                        "Large-bound problem evidence plus TLE or step-by-step simulation should be treated as a scale diagnosis before a strategy diagnosis.",
                        "Empty or minimum-input runtime evidence should be treated as a boundary diagnosis before a generic runtime diagnosis.",
                        "Learning trajectory can explain repeated or regressed behavior but must not invent the current bug.",
                        "Previous learning action feedback is process evidence: OBSERVED supports review or transfer, PARTIALLY_OBSERVED supports a smaller checkable task, CONTRADICTED supports lower hint granularity or teacher attention, and NOT_OBSERVED must not be treated as completed work."
                ))
                .tagSelectionRules(List.of(
                        "Select primaryIssueTag from issueTags only.",
                        "Select fineGrainedTag only when evidence directly distinguishes it from its parent issue.",
                        "If a fineGrainedTag is selected, its evidenceRefs must support that fine-grained diagnosis.",
                        "Do not select NEEDS_MORE_EVIDENCE when a high-confidence candidateSignal directly explains the visible failed case.",
                        "Do not select a tag solely because it appears in learningMemorySummary; it must also fit current submission evidence or be described as follow-up attention.",
                        "Prefer SAMPLE_OVERFIT only when public samples pass and hidden failures are observed; describe it as a need for self-made counterexamples, not as hidden-case knowledge.",
                        "If no candidate is sufficiently supported, select NEEDS_MORE_EVIDENCE when available."
                ))
                .conflictRules(List.of(
                        "When candidate signals conflict, cite the conflict in uncertainty and choose NEEDS_MORE_EVIDENCE unless one signal has stronger concrete evidence.",
                        "When hidden failures are observed without visible hidden data, describe the missing evidence instead of guessing a hidden case.",
                        "When code evidence and judge output point to different issues, prefer the issue explaining the first failed observable behavior."
                ))
                .teachingActionRules(List.of(
                        "Bind teachingAction to the selected diagnosis tag when possible.",
                        "Use COLLECT_EVIDENCE when primaryIssueTag is NEEDS_MORE_EVIDENCE.",
                        "The teaching action should create one small observable student task, not a full fix.",
                        "The student task must cite one evidence anchor and ask the student to inspect a line, state, counterexample, or input-output mismatch.",
                        "When learning memory shows repeated stuck behavior or ineffective previous intervention, reduce the task size or change the teaching action instead of repeating the same hint.",
                        "When previous learning action feedback is CONTRADICTED, shrink the next action into a minimal case, variable trace, or teacher-checkable artifact instead of repeating the same broad hint.",
                        "When previous learning action feedback is OBSERVED, shift toward review, transfer, or explaining why the fix generalizes."
                ))
                .build();
    }

    private StandardLibraryPack.StudentFeedbackRules buildStudentFeedbackRules() {
        return StandardLibraryPack.StudentFeedbackRules.builder()
                .blockingIssueRules(List.of(
                        "blockingIssues must explain the current verdict, firstFailedCase, compiler error, or runtime error before any broad improvement.",
                        "The first blocking issue is the student's next priority and must include evidenceRefs from the brief.",
                        "Do not put code style, elegance, or optional optimization into blockingIssues unless it directly caused the current failure."
                ))
                .secondaryIssueRules(List.of(
                        "secondaryIssues may mention plausible non-primary signals only when they do not outrank the first failed observable behavior.",
                        "If a signal is distracting, explain why it is not primary instead of treating it as a fix target."
                ))
                .improvementRules(List.of(
                        "improvementOpportunities must use standardLibrary.improvementTags ids only.",
                        "Improvements are follow-up learning value after or alongside the current fix, not the primary cause.",
                        "Prefer testing habit, complexity estimate, code clarity, boundary awareness, robustness, and debug cleanup."
                ))
                .nextActionRules(List.of(
                        "nextLearningAction must be one observable comparison, trace, estimate, counterexample, or checklist task.",
                        "Do not provide a full solution, replacement loop header, transition formula, or executable control structure."
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
            case "FIX_FIRST_COMPILER_ERROR" -> "Use when compilation or syntax evidence blocks deeper reasoning.";
            case "ASK_MIN_CASE" -> "Use when boundary or special cases are likely.";
            case "TRACE_VARIABLES" -> "Use when loop variables, indexes, or counters may be wrong.";
            case "COMPARE_OUTPUT" -> "Use when visible output differs from expected output.";
            case "COMPARE_INPUT_SPEC" -> "Use when the input structure may not match the problem statement.";
            case "CHECK_BRANCH_COVERAGE" -> "Use when conditions or branch order may miss a case.";
            case "COMPARE_STRUCTURES" -> "Use when a data structure choice affects correctness or scale.";
            case "COUNT_COMPLEXITY" -> "Use when time or scale is the key evidence.";
            case "TRACE_STATE" -> "Use when initial values or resets may be wrong.";
            case "DEFINE_STATE" -> "Use when state meaning or transition is unclear.";
            case "DRAW_RECURSION_TREE" -> "Use when recursion exit or backtracking behavior needs inspection.";
            case "BUILD_COUNTEREXAMPLE" -> "Use when generality or greedy assumptions need testing.";
            case "CHECK_INVARIANT" -> "Use when an algorithmic assumption or greedy rule needs proof.";
            case "EXPLAIN_GENERALITY" -> "Use when the submission passed but the student should explain transferability.";
            case "COMPARE_SUBMISSIONS" -> "Use when a recent fix may have introduced regression.";
            case "CHECK_RUNTIME_GUARDS" -> "Use when runtime stability is the current failure.";
            case "COLLECT_EVIDENCE" -> "Use when available evidence is insufficient.";
            default -> "Use when this action best matches the selected diagnosis tag.";
        };
    }

    private String resolveStudentTaskTemplate(String action) {
        return switch (action) {
            case "FIX_FIRST_COMPILER_ERROR" -> "Read the first compiler or runtime message and explain what it points to.";
            case "ASK_MIN_CASE" -> "Construct one minimal input and manually check the expected behavior.";
            case "TRACE_VARIABLES" -> "Trace the key variables for the first and last loop iteration.";
            case "COMPARE_OUTPUT" -> "Compare actual and expected output character by character.";
            case "COMPARE_INPUT_SPEC" -> "Circle the input format in the statement and match each token to one read operation.";
            case "CHECK_BRANCH_COVERAGE" -> "List the condition categories and mark which branch handles each one.";
            case "COMPARE_STRUCTURES" -> "Compare two candidate structures by operation cost and required information.";
            case "COUNT_COMPLEXITY" -> "Estimate the number of core operations at maximum input size.";
            case "TRACE_STATE" -> "Write the state before the loop, after one iteration, and after a reset point.";
            case "DEFINE_STATE" -> "Write what each state means before and after one transition.";
            case "DRAW_RECURSION_TREE" -> "Draw the first two recursion levels and mark where recursion stops.";
            case "BUILD_COUNTEREXAMPLE" -> "Try to build a small case where the current assumption fails.";
            case "CHECK_INVARIANT" -> "State the assumption that should stay true after each step, then test a small counterexample.";
            case "EXPLAIN_GENERALITY" -> "Explain why the current solution works beyond the sample cases.";
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
