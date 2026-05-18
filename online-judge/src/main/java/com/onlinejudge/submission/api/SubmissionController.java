package com.onlinejudge.submission.api;

import com.onlinejudge.submission.dto.SubmissionAnalysisLookupResponse;
import com.onlinejudge.submission.dto.SubmissionComparisonResponse;
import com.onlinejudge.submission.dto.SubmissionHistorySummaryResponse;
import com.onlinejudge.submission.dto.SubmissionRequest;
import com.onlinejudge.submission.dto.SubmissionResponse;
import com.onlinejudge.classroom.application.CoachPromptService;
import com.onlinejudge.classroom.dto.CoachPromptResponse;
import com.onlinejudge.classroom.dto.CoachReplyRequest;
import com.onlinejudge.submission.application.JudgeService;
import com.onlinejudge.submission.application.SubmissionAnalysisService;
import com.onlinejudge.submission.application.SubmissionAnalysisAsyncService;
import com.onlinejudge.submission.application.SubmissionComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final JudgeService judgeService;
    private final SubmissionAnalysisService submissionAnalysisService;
    private final SubmissionAnalysisAsyncService submissionAnalysisAsyncService;
    private final SubmissionComparisonService submissionComparisonService;
    private final CoachPromptService coachPromptService;

    @PostMapping
    public ResponseEntity<SubmissionResponse> submitCode(@Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(judgeService.submitCode(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getSubmission(@PathVariable Long id) {
        return ResponseEntity.ok(judgeService.getSubmission(id));
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<SubmissionAnalysisLookupResponse> getSubmissionAnalysis(@PathVariable Long id) {
        SubmissionAnalysisLookupResponse lookup = submissionAnalysisService.getSubmissionAnalysisLookup(id);
        if (lookup.getAnalysis() == null) {
            submissionAnalysisAsyncService.enqueue(id);
            return ResponseEntity.accepted().body(lookup);
        }
        return ResponseEntity.ok(lookup);
    }

    @PostMapping("/{id}/analysis")
    public ResponseEntity<SubmissionAnalysisLookupResponse> triggerSubmissionAnalysis(@PathVariable Long id) {
        SubmissionAnalysisLookupResponse lookup = submissionAnalysisService.getSubmissionAnalysisLookup(id);
        if (lookup.getAnalysis() == null) {
            submissionAnalysisAsyncService.enqueue(id);
            return ResponseEntity.accepted().body(lookup);
        }
        return ResponseEntity.ok(lookup);
    }

    @GetMapping("/{id}/coach-prompt")
    public ResponseEntity<CoachPromptResponse> getCoachPrompt(@PathVariable Long id) {
        CoachPromptResponse prompt = coachPromptService.getLatestPrompt(id);
        if (prompt == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(prompt);
    }

    @PostMapping("/{id}/coach-prompt")
    public ResponseEntity<CoachPromptResponse> generateCoachPrompt(@PathVariable Long id) {
        return ResponseEntity.ok(coachPromptService.generateNextQuestion(id));
    }

    @PostMapping("/{id}/coach-turns")
    public ResponseEntity<CoachPromptResponse> replyCoachPrompt(@PathVariable Long id,
                                                                @Valid @RequestBody CoachReplyRequest request) {
        return ResponseEntity.ok(coachPromptService.replyAndGenerateNext(id, request));
    }

    @GetMapping("/problem/{problemId}/history-summary")
    public ResponseEntity<List<SubmissionHistorySummaryResponse>> getSubmissionHistorySummary(@PathVariable Long problemId) {
        return ResponseEntity.ok(submissionAnalysisService.getSubmissionHistorySummaries(problemId));
    }

    @GetMapping("/compare")
    public ResponseEntity<SubmissionComparisonResponse> compareSubmissions(@RequestParam Long leftId,
                                                                           @RequestParam Long rightId) {
        return ResponseEntity.ok(submissionComparisonService.compare(leftId, rightId));
    }

}
