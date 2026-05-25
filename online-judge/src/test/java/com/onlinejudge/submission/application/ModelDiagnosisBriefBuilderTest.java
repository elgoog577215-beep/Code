package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelDiagnosisBriefBuilderTest {

    private final ModelDiagnosisBriefBuilder briefBuilder = new ModelDiagnosisBriefBuilder();

    @Test
    void briefRedactsHiddenFailedCaseData() {
        DiagnosisEvidencePackage evidencePackage = DiagnosisEvidencePackage.builder()
                .problem(problem())
                .submission(submission())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .hiddenFailureObserved(true)
                        .firstFailedCase(SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                                .testCaseNumber(7)
                                .hidden(true)
                                .input("secret input")
                                .expectedOutput("secret expected")
                                .actualOutput("secret actual")
                                .build())
                        .caseResultsSummary(List.of(DiagnosisEvidencePackage.CaseSummary.builder()
                                .testCaseNumber(7)
                                .hidden(true)
                                .passed(false)
                                .actualOutputPreview("secret actual")
                                .expectedOutputPreview("secret expected")
                                .build()))
                        .build())
                .build();

        ModelDiagnosisBrief brief = briefBuilder.build(evidencePackage, ruleSignals(), null);

        assertThat(brief.getFirstFailedCase()).isNotNull();
        assertThat(brief.getFirstFailedCase().isHidden()).isTrue();
        assertThat(brief.getFirstFailedCase().getInput()).isNull();
        assertThat(brief.getFirstFailedCase().getExpectedOutput()).isNull();
        assertThat(brief.getFirstFailedCase().getActualOutput()).isNull();
        assertThat(brief.getVisibleCaseFacts()).singleElement()
                .satisfies(fact -> {
                    assertThat(fact.getHidden()).isTrue();
                    assertThat(fact.getActualOutputPreview()).isNull();
                    assertThat(fact.getExpectedOutputPreview()).isNull();
                });
        assertThat(brief.getHiddenDataBoundary().getHiddenFailureObserved()).isTrue();
        assertThat(brief.getHiddenDataBoundary().getHiddenInputVisible()).isFalse();
        assertThat(brief.getUncertainty()).contains("Hidden failure");
    }

    @Test
    void briefCarriesCandidateSignalsAndEvidenceRefs() {
        ModelDiagnosisBrief brief = briefBuilder.build(
                DiagnosisEvidencePackage.builder()
                        .problem(problem())
                        .submission(submission())
                        .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                                .firstFailedCase(SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                                        .testCaseNumber(1)
                                        .hidden(false)
                                        .input("3")
                                        .expectedOutput("6")
                                        .actualOutput("3")
                                        .build())
                                .build())
                        .build(),
                ruleSignals(),
                SubmissionAnalysisResponse.builder()
                        .issueTags(List.of("BOUNDARY_CONDITION"))
                        .fineGrainedTags(List.of("OFF_BY_ONE"))
                        .evidenceRefs(List.of("baseline:teacher"))
                        .build()
        );

        assertThat(brief.getCandidateSignals()).hasSize(1);
        assertThat(brief.getCandidateSignals().get(0).getEvidenceRef()).isEqualTo("code:range_excludes_n");
        assertThat(brief.getAllowedIssueTags()).contains("LOOP_BOUNDARY", "BOUNDARY_CONDITION", "NEEDS_MORE_EVIDENCE");
        assertThat(brief.getAllowedFineGrainedTags()).contains("OFF_BY_ONE");
        assertThat(brief.getEvidenceRefs()).contains("code:range_excludes_n", "judge:first_failed_case", "baseline:teacher");
        assertThat(brief.getKeyCodeExcerpt()).contains("1:");
    }

    @Test
    void standardLibraryPackKeepsCandidateTagsAndUncertaintyExit() {
        ModelDiagnosisBrief brief = briefBuilder.build(
                DiagnosisEvidencePackage.builder()
                        .problem(problem())
                        .submission(submission())
                        .build(),
                ruleSignals(),
                null
        );

        StandardLibraryPack pack = new StandardLibraryPackBuilder(new DiagnosisTaxonomy()).build(brief, ruleSignals());

        assertThat(pack.getIssueTags()).extracting(StandardLibraryPack.TagOption::getId)
                .contains("LOOP_BOUNDARY", "NEEDS_MORE_EVIDENCE");
        assertThat(pack.getFineGrainedTags()).extracting(StandardLibraryPack.TagOption::getId)
                .contains("OFF_BY_ONE");
        assertThat(pack.getTeachingActions()).extracting(StandardLibraryPack.TeachingActionOption::getId)
                .contains("TRACE_VARIABLES", "COLLECT_EVIDENCE");
        assertThat(pack.getSafetyRules()).anyMatch(rule -> rule.contains("hidden test data"));
        assertThat(pack.getUncertaintyOptions()).contains("NEEDS_MORE_EVIDENCE");
    }

    @Test
    void briefCarriesLearningMemoryAsAuxiliaryEvidenceWithoutDroppingCurrentSignals() {
        DiagnosisEvidencePackage evidencePackage = DiagnosisEvidencePackage.builder()
                .problem(problem())
                .submission(submission())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .firstFailedCase(SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                                .testCaseNumber(1)
                                .hidden(false)
                                .input("3")
                                .expectedOutput("6")
                                .actualOutput("3")
                                .build())
                        .build())
                .learningMemory(DiagnosisEvidencePackage.StudentLearningMemorySnapshot.builder()
                        .studentProfileId(9L)
                        .observedSubmissionCount(5)
                        .recurringIssueTags(List.of(DiagnosisEvidencePackage.MemoryTagStat.builder()
                                .tag("IO_FORMAT")
                                .count(3L)
                                .evidenceSubmissionIds(List.of(101L, 102L, 103L))
                                .build()))
                        .recurringFineGrainedTags(List.of(DiagnosisEvidencePackage.MemoryTagStat.builder()
                                .tag("INPUT_PARSING")
                                .count(3L)
                                .evidenceSubmissionIds(List.of(101L, 102L, 103L))
                                .build()))
                        .abilityFocus(List.of(DiagnosisEvidencePackage.AbilityFocus.builder()
                                .abilityPoint("题意读取")
                                .submissionCount(3L)
                                .problemCount(2L)
                                .evidenceTags(List.of("IO_FORMAT", "INPUT_PARSING"))
                                .build()))
                        .recentTrend("历史中多次出现输入读取问题")
                        .evidenceRefs(List.of("memory:student:9", "memory:recurring_issue:IO_FORMAT"))
                        .build())
                .build();

        ModelDiagnosisBrief brief = briefBuilder.build(evidencePackage, ruleSignals(), null);

        assertThat(brief.getLearningMemorySummary())
                .contains("observedSubmissions=5")
                .contains("recurringIssueTags")
                .contains("abilityFocus");
        assertThat(brief.getEvidenceRefs())
                .contains("judge:first_failed_case", "code:range_excludes_n", "memory:student:9");
        assertThat(brief.getAllowedIssueTags()).contains("LOOP_BOUNDARY", "IO_FORMAT", "NEEDS_MORE_EVIDENCE");
        assertThat(brief.getAllowedFineGrainedTags()).contains("OFF_BY_ONE", "INPUT_PARSING");
        assertThat(brief.getCandidateSignals())
                .anySatisfy(signal -> {
                    assertThat(signal.getEvidenceRef()).isEqualTo("code:range_excludes_n");
                    assertThat(signal.getConfidence()).isGreaterThan(0.8);
                })
                .anySatisfy(signal -> {
                    assertThat(signal.getEvidenceRef()).isEqualTo("memory:recurring_issue:IO_FORMAT");
                    assertThat(signal.getConfidence()).isLessThan(0.6);
                    assertThat(signal.getReason()).contains("auxiliary");
                });
    }

    private DiagnosisEvidencePackage.ProblemEvidence problem() {
        return DiagnosisEvidencePackage.ProblemEvidence.builder()
                .id(1L)
                .title("Sum 1 to n")
                .description("Input n and output the sum from 1 to n.")
                .timeLimit(1000)
                .memoryLimit(65536)
                .knowledgePoints(List.of("loop"))
                .commonMistakes(List.of("off by one"))
                .build();
    }

    private DiagnosisEvidencePackage.SubmissionEvidence submission() {
        return DiagnosisEvidencePackage.SubmissionEvidence.builder()
                .id(11L)
                .language("Python 3")
                .verdict("WRONG_ANSWER")
                .sourceCode("""
                        n = int(input())
                        s = 0
                        for i in range(1, n):
                            s += i
                        print(s)
                        """)
                .sourceCodeWithLineNumbers("""
                        1: n = int(input())
                        2: s = 0
                        3: for i in range(1, n):
                        4:     s += i
                        5: print(s)
                        """)
                .sourceCodeLineCount(5)
                .build();
    }

    private RuleSignalAnalyzer.RuleSignalResult ruleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("LOOP_BOUNDARY"))
                .candidateFineGrainedTags(List.of("OFF_BY_ONE"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .signals(List.of(RuleSignalAnalyzer.Signal.builder()
                        .evidenceRef("code:range_excludes_n")
                        .coarseTag("LOOP_BOUNDARY")
                        .fineTag("OFF_BY_ONE")
                        .confidence(0.91)
                        .message("range(1, n) excludes n")
                        .build()))
                .build();
    }
}
