package com.onlinejudge.problem.api;

import com.onlinejudge.problem.dto.CreateProblemRequest;
import com.onlinejudge.problem.dto.ProblemAttachmentResponse;
import com.onlinejudge.problem.dto.StatementImportRequest;
import com.onlinejudge.problem.dto.StatementImportResponse;
import com.onlinejudge.problem.dto.TestDataFilePreviewResponse;
import com.onlinejudge.problem.dto.TestDataImportCommitResponse;
import com.onlinejudge.problem.dto.TestDataImportPreviewResponse;
import com.onlinejudge.report.dto.GrowthReportResponse;
import com.onlinejudge.problem.dto.ProblemCatalogItemResponse;
import com.onlinejudge.problem.dto.ProblemManageResponse;
import com.onlinejudge.problem.dto.ProblemResponse;
import com.onlinejudge.report.application.GrowthReportService;
import com.onlinejudge.problem.application.ProblemTestDataImportService;
import com.onlinejudge.problem.application.ProblemAttachmentService;
import com.onlinejudge.problem.application.ProblemService;
import com.onlinejudge.problem.application.StatementImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final GrowthReportService growthReportService;
    private final ProblemTestDataImportService testDataImportService;
    private final ProblemAttachmentService attachmentService;
    private final StatementImportService statementImportService;

    @GetMapping
    public ResponseEntity<List<ProblemResponse>> getAllProblems() {
        return ResponseEntity.ok(problemService.getAllProblems());
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<ProblemCatalogItemResponse>> getProblemCatalog() {
        return ResponseEntity.ok(problemService.getProblemCatalog());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponse> getProblemById(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getProblemById(id));
    }

    @GetMapping("/{id}/manage")
    public ResponseEntity<ProblemManageResponse> getProblemForManage(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getProblemForManage(id));
    }

    @PostMapping
    public ResponseEntity<ProblemResponse> createProblem(@Valid @RequestBody CreateProblemRequest request) {
        return ResponseEntity.ok(problemService.createProblem(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemResponse> updateProblem(@PathVariable Long id,
                                                         @Valid @RequestBody CreateProblemRequest request) {
        return ResponseEntity.ok(problemService.updateProblem(id, request));
    }

    @PostMapping("/statement-import")
    public ResponseEntity<StatementImportResponse> importStatement(@RequestBody StatementImportRequest request) {
        return ResponseEntity.ok(statementImportService.parseMarkdown(request.getContent()));
    }

    @PostMapping(value = "/{problemId}/test-data/import-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestDataImportPreviewResponse> previewTestDataImport(@PathVariable Long problemId,
                                                                               @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(testDataImportService.preview(problemId, file));
    }

    @PostMapping(value = "/{problemId}/test-data/import-commit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestDataImportCommitResponse> commitTestDataImport(@PathVariable Long problemId,
                                                                             @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(testDataImportService.commit(problemId, file));
    }

    @GetMapping("/{problemId}/test-data/{testCaseId}/preview")
    public ResponseEntity<TestDataFilePreviewResponse> previewStoredTestData(@PathVariable Long problemId,
                                                                             @PathVariable Long testCaseId,
                                                                             @RequestParam(defaultValue = "input") String kind) {
        return ResponseEntity.ok(testDataImportService.previewStoredFile(problemId, testCaseId, kind));
    }

    @GetMapping("/{problemId}/test-data/download")
    public ResponseEntity<byte[]> downloadTestData(@PathVariable Long problemId) {
        byte[] payload = testDataImportService.downloadPackage(problemId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"problem-" + problemId + "-test-data.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(payload.length)
                .body(payload);
    }

    @GetMapping("/{problemId}/attachments")
    public ResponseEntity<List<ProblemAttachmentResponse>> listAttachments(@PathVariable Long problemId) {
        return ResponseEntity.ok(attachmentService.list(problemId));
    }

    @PostMapping(value = "/{problemId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProblemAttachmentResponse> uploadAttachment(@PathVariable Long problemId,
                                                                      @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(attachmentService.upload(problemId, file));
    }

    @GetMapping("/{problemId}/attachments/{attachmentId}/download")
    public ResponseEntity<?> downloadAttachment(@PathVariable Long problemId,
                                                @PathVariable String attachmentId) {
        ProblemAttachmentService.DownloadedAttachment attachment = attachmentService.download(problemId, attachmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .contentLength(attachment.getSizeBytes())
                .body(attachment.getResource());
    }

    @GetMapping("/{problemId}/growth-report")
    public ResponseEntity<GrowthReportResponse> getGrowthReport(@PathVariable Long problemId) {
        return ResponseEntity.ok(growthReportService.buildGrowthReport(problemId));
    }

    @GetMapping("/{problemId}/growth-report/export")
    public ResponseEntity<byte[]> exportGrowthReport(@PathVariable Long problemId,
                                                     @RequestParam(defaultValue = "markdown") String format) {
        byte[] payload = growthReportService.exportGrowthReport(problemId, format);
        String normalizedFormat = format == null ? "markdown" : format.trim().toLowerCase();
        String extension = normalizedFormat.equals("pdf") ? "pdf" : "md";
        MediaType mediaType = normalizedFormat.equals("pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("text/markdown;charset=UTF-8");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"problem-" + problemId + "-growth-report." + extension + "\"")
                .contentType(mediaType)
                .contentLength(payload.length)
                .body(payload);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProblem(@PathVariable Long id) {
        var deletedProblem = problemService.deleteProblem(id);
        return ResponseEntity.ok(Map.of(
                "message", "题目删除成功",
                "title", deletedProblem.getTitle()
        ));
    }
}

