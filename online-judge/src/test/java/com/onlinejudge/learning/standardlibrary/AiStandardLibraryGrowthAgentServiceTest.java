package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryGrowthAgentService;
import com.onlinejudge.learning.standardlibrary.application.StandardLibraryGrowthProposal;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryGrowthCandidate;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryGrowthCandidateStatus;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryGrowthCandidateRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-standard-library-growth-agent;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false",
        "ai.standard-library-growth.auto-merge-enabled=false"
})
class AiStandardLibraryGrowthAgentServiceTest {

    @Autowired
    AiStandardLibraryGrowthAgentService service;

    @Autowired
    AiStandardLibraryGrowthCandidateRepository candidateRepository;

    @Autowired
    AiStandardLibraryItemRepository itemRepository;

    @Test
    void storesGrowthProposalAsReviewCandidateWithoutChangingFormalLibrary() {
        long formalCountBefore = itemRepository.count();

        AiStandardLibraryGrowthCandidate saved = service.propose(StandardLibraryGrowthProposal.builder()
                .suggestedCode("MP_LOOP_RIGHT_ENDPOINT_BY_VISIBLE_FAILED_CASE")
                .suggestedName("由可见失败样例暴露的循环右端漏取")
                .layer(AiStandardLibraryLayer.MISTAKE_POINT)
                .suggestedPath(List.of("BASIC", "LOOP", "BOUNDARY", "VISIBLE_CASE_ENDPOINT"))
                .sourceProblemId(1L)
                .sourceSubmissionId(11L)
                .similarExistingItemCodes(List.of("MP_RANGE_RIGHT_ENDPOINT_MISSING"))
                .changeReason("现有库命中上级循环边界，但没有表达可见失败样例如何定位端点漏取。")
                .evidenceRefs(List.of("code:range_excludes_n", "judge:first_failed_case"))
                .confidence(0.86)
                .build());

        assertThat(saved.getStatus()).isEqualTo(AiStandardLibraryGrowthCandidateStatus.PROPOSED);
        assertThat(saved.getSuggestedCode()).isEqualTo("MP_LOOP_RIGHT_ENDPOINT_BY_VISIBLE_FAILED_CASE");
        assertThat(saved.getSuggestedPath()).contains("BASIC.LOOP.BOUNDARY.VISIBLE_CASE_ENDPOINT");
        assertThat(saved.getSourceProblemId()).isEqualTo(1L);
        assertThat(saved.getSourceSubmissionId()).isEqualTo(11L);
        assertThat(saved.getSimilarExistingItems()).contains("MP_RANGE_RIGHT_ENDPOINT_MISSING");
        assertThat(saved.getRollbackInfo()).contains("候选尚未写入正式标准库");
        assertThat(itemRepository.count()).isEqualTo(formalCountBefore);
    }

    @Test
    void precheckRejectsDuplicateFormalItemCode() {
        var existing = itemRepository.findByLayerAndCode(AiStandardLibraryLayer.MISTAKE_POINT, "IO_FORMAT").orElseThrow();

        StandardLibraryGrowthProposal proposal = StandardLibraryGrowthProposal.builder()
                .suggestedCode(existing.getCode())
                .suggestedName("重复正式条目")
                .layer(existing.getLayer())
                .suggestedPath(List.of("BASIC", "IO"))
                .sourceProblemId(1L)
                .sourceSubmissionId(11L)
                .changeReason("尝试重复已有正式条目。")
                .evidenceRefs(List.of("code:range_excludes_n"))
                .confidence(0.9)
                .build();

        AiStandardLibraryGrowthCandidate saved = service.propose(proposal);

        assertThat(saved.getStatus()).isEqualTo(AiStandardLibraryGrowthCandidateStatus.BLOCKED);
        assertThat(saved.getPrecheckMessage()).contains("正式标准库已存在");
        assertThat(candidateRepository.findAll()).filteredOn(candidate -> candidate.getSuggestedCode().equals(existing.getCode()))
                .singleElement()
                .satisfies(candidate -> assertThat(candidate.getStatus()).isEqualTo(AiStandardLibraryGrowthCandidateStatus.BLOCKED));
    }

    @Test
    void precheckRejectsMissingSourceAndLowConfidence() {
        AiStandardLibraryGrowthCandidate saved = service.propose(StandardLibraryGrowthProposal.builder()
                .suggestedCode("MP_LOW_CONFIDENCE_WITHOUT_SOURCE")
                .suggestedName("缺少来源且置信度不足")
                .layer(AiStandardLibraryLayer.MISTAKE_POINT)
                .suggestedPath(List.of("BASIC", "LOOP"))
                .changeReason("缺少来源时不能进入待审。")
                .confidence(0.41)
                .build());

        assertThat(saved.getStatus()).isEqualTo(AiStandardLibraryGrowthCandidateStatus.BLOCKED);
        assertThat(saved.getPrecheckMessage()).contains("来源题目或提交缺失").contains("置信度不足");
    }

    @Test
    void proposesCandidatesFromDiagnosisReportOutput() {
        var output = com.onlinejudge.submission.application.AdviceGenerationOutput.builder()
                .libraryGrowth(com.onlinejudge.submission.application.AdviceGenerationOutput.LibraryGrowth.builder()
                        .candidates(List.of(com.onlinejudge.submission.application.AdviceGenerationOutput.LibraryGrowthCandidate.builder()
                                .name("滑动窗口右端扩张后未及时更新答案")
                                .suggestedPath(List.of("ALGO", "TWO_POINTERS", "WINDOW", "ANSWER_UPDATE"))
                                .sourceProblemId(2L)
                                .sourceSubmissionId(22L)
                                .similarExistingItems(List.of("MP_ALGO_TWO_POINTERS_WINDOW"))
                                .reason("诊断报告判定现有标准库为 PARTIAL，需要补充更细颗粒窗口答案更新错因。")
                                .status("PROPOSED")
                                .confidence(0.78)
                                .build()))
                        .build())
                .build();

        List<AiStandardLibraryGrowthCandidate> saved = service.proposeFromDiagnosisOutput(output);

        assertThat(saved).singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.getSuggestedCode()).startsWith("MP_ALGO_TWO_POINTERS_WINDOW_ANSWER_UPDATE");
                    assertThat(candidate.getStatus()).isEqualTo(AiStandardLibraryGrowthCandidateStatus.PROPOSED);
                    assertThat(candidate.getSourceProblemId()).isEqualTo(2L);
                    assertThat(candidate.getSourceSubmissionId()).isEqualTo(22L);
                    assertThat(candidate.getChangeReason()).contains("PARTIAL");
                });
    }

    @Test
    void autoMergeIsBlockedWhenConfigurationDisallowsFormalWrites() {
        AiStandardLibraryGrowthCandidate candidate = service.propose(StandardLibraryGrowthProposal.builder()
                .suggestedCode("MP_BLOCKED_AUTO_MERGE")
                .suggestedName("配置关闭时不能自动入库")
                .layer(AiStandardLibraryLayer.MISTAKE_POINT)
                .suggestedPath(List.of("BASIC", "LOOP", "BOUNDARY"))
                .sourceProblemId(3L)
                .sourceSubmissionId(33L)
                .changeReason("用于验证自动入库配置门禁。")
                .evidenceRefs(List.of("code:loop_boundary"))
                .confidence(0.91)
                .build());

        AiStandardLibraryGrowthCandidate merged = service.autoMergeToFormalLibrary(candidate.getId(), null);

        assertThat(merged.getStatus()).isEqualTo(AiStandardLibraryGrowthCandidateStatus.NEEDS_REVIEW);
        assertThat(merged.getPrecheckMessage()).contains("自动入库未开启");
        assertThat(itemRepository.existsByLayerAndCode(AiStandardLibraryLayer.MISTAKE_POINT, "MP_BLOCKED_AUTO_MERGE")).isFalse();
        assertThat(merged.getRollbackInfo()).contains("未写入正式标准库");
    }

    @Test
    void ignoresCandidateWithoutChangingFormalLibrary() {
        AiStandardLibraryGrowthCandidate candidate = service.propose(StandardLibraryGrowthProposal.builder()
                .suggestedCode("MP_IGNORE_CANDIDATE")
                .suggestedName("可忽略候选")
                .layer(AiStandardLibraryLayer.MISTAKE_POINT)
                .suggestedPath(List.of("BASIC", "IO", "FORMAT"))
                .sourceProblemId(4L)
                .sourceSubmissionId(44L)
                .changeReason("老师认为暂不需要入库。")
                .evidenceRefs(List.of("judge:wrong_answer"))
                .confidence(0.81)
                .build());

        long formalCountBefore = itemRepository.count();

        AiStandardLibraryGrowthCandidate ignored = service.ignore(candidate.getId(), "与已有课堂讲解重复");

        assertThat(ignored.getStatus()).isEqualTo(AiStandardLibraryGrowthCandidateStatus.IGNORED);
        assertThat(ignored.getPrecheckMessage()).contains("与已有课堂讲解重复");
        assertThat(ignored.getRollbackInfo()).contains("未写入正式标准库");
        assertThat(itemRepository.count()).isEqualTo(formalCountBefore);
    }
}
