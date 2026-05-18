package com.onlinejudge.classroom.api;

import com.onlinejudge.classroom.application.ClassroomService;
import com.onlinejudge.classroom.application.AiQualityOverviewService;
import com.onlinejudge.classroom.application.StudentAbilityProfileService;
import com.onlinejudge.classroom.application.StudentIdentityAuditService;
import com.onlinejudge.classroom.application.StudentRecommendationService;
import com.onlinejudge.classroom.application.StudentTrajectoryService;
import com.onlinejudge.classroom.dto.*;
import com.onlinejudge.classroom.importing.ClassroomImportService;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;
    private final AiQualityOverviewService aiQualityOverviewService;
    private final ClassroomImportService classroomImportService;
    private final StudentTrajectoryService studentTrajectoryService;
    private final StudentAbilityProfileService studentAbilityProfileService;
    private final StudentIdentityAuditService studentIdentityAuditService;
    private final StudentRecommendationService studentRecommendationService;
    private final DiagnosisTaxonomy diagnosisTaxonomy;

    @GetMapping("/api/teacher/classes")
    public ResponseEntity<List<ClassGroupResponse>> getClasses() {
        return ResponseEntity.ok(classroomService.getClassGroups());
    }

    @PostMapping("/api/teacher/classes")
    public ResponseEntity<ClassGroupResponse> createClass(@Valid @RequestBody CreateClassGroupRequest request) {
        return ResponseEntity.ok(classroomService.createClassGroup(request));
    }

    @GetMapping("/api/teacher/classes/{classGroupId}/identity-audit")
    public ResponseEntity<StudentIdentityAuditResponse> getStudentIdentityAudit(@PathVariable Long classGroupId) {
        return ResponseEntity.ok(studentIdentityAuditService.auditClass(classGroupId));
    }

    @GetMapping("/api/teacher/assignments")
    public ResponseEntity<List<AssignmentResponse>> getAssignments() {
        return ResponseEntity.ok(classroomService.getAssignments());
    }

    @GetMapping("/api/teacher/assignments/{assignmentId}")
    public ResponseEntity<AssignmentResponse> getAssignment(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(classroomService.getAssignment(assignmentId));
    }

    @PostMapping("/api/teacher/assignments")
    public ResponseEntity<AssignmentResponse> createAssignment(@Valid @RequestBody CreateAssignmentRequest request) {
        return ResponseEntity.ok(classroomService.createAssignment(request));
    }

    @PutMapping("/api/teacher/assignments/{assignmentId}")
    public ResponseEntity<AssignmentResponse> updateAssignment(@PathVariable Long assignmentId,
                                                               @Valid @RequestBody CreateAssignmentRequest request) {
        return ResponseEntity.ok(classroomService.updateAssignment(assignmentId, request));
    }

    @PostMapping("/api/teacher/assignments/{assignmentId}/invite")
    public ResponseEntity<AssignmentResponse> rotateInvite(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(classroomService.rotateInvite(assignmentId));
    }

    @PostMapping("/api/teacher/classes/import-preview")
    public ResponseEntity<ImportPreviewResponse> previewClassImport(@RequestBody ImportRequest request) {
        return ResponseEntity.ok(classroomImportService.previewStudents(request));
    }

    @PostMapping("/api/teacher/classes/import-commit")
    public ResponseEntity<ImportCommitResponse> commitClassImport(@RequestBody ImportRequest request) {
        return ResponseEntity.ok(classroomImportService.commitStudents(request));
    }

    @PostMapping("/api/teacher/problems/import-preview")
    public ResponseEntity<ImportPreviewResponse> previewProblemImport(@RequestBody ImportRequest request) {
        return ResponseEntity.ok(classroomImportService.previewProblems(request));
    }

    @PostMapping("/api/teacher/problems/import-commit")
    public ResponseEntity<ImportCommitResponse> commitProblemImport(@RequestBody ImportRequest request) {
        return ResponseEntity.ok(classroomImportService.commitProblems(request));
    }

    @GetMapping("/api/teacher/assignments/{assignmentId}/overview")
    public ResponseEntity<AssignmentOverviewResponse> getAssignmentOverview(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(classroomService.getAssignmentOverview(assignmentId));
    }

    @GetMapping("/api/teacher/assignments/{assignmentId}/ai-quality")
    public ResponseEntity<AiQualityOverviewResponse> getAiQualityOverview(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(aiQualityOverviewService.buildOverview(assignmentId));
    }

    @PostMapping("/api/teacher/assignments/{assignmentId}/diagnosis-corrections")
    public ResponseEntity<TeacherDiagnosisCorrectionResponse> correctDiagnosis(@PathVariable Long assignmentId,
                                                                               @Valid @RequestBody TeacherDiagnosisCorrectionRequest request) {
        return ResponseEntity.ok(classroomService.correctDiagnosis(assignmentId, request));
    }

    @GetMapping("/api/teacher/assignments/{assignmentId}/diagnosis-eval-candidates")
    public ResponseEntity<DiagnosisEvalCandidateResponse> getDiagnosisEvalCandidates(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(classroomService.getDiagnosisEvalCandidates(assignmentId));
    }

    @GetMapping("/api/teacher/diagnosis-tags")
    public ResponseEntity<List<DiagnosisTagResponse>> getDiagnosisTags() {
        return ResponseEntity.ok(diagnosisTaxonomy.allTags()
                .stream()
                .map(DiagnosisTagResponse::from)
                .toList());
    }

    @PostMapping("/api/invites/resolve")
    public ResponseEntity<AssignmentResponse> resolveInvite(@Valid @RequestBody InviteResolveRequest request) {
        return ResponseEntity.ok(classroomService.resolveInvite(request.getCode()));
    }

    @GetMapping("/api/student/assignments/{inviteCode}")
    public ResponseEntity<AssignmentResponse> getStudentAssignment(@PathVariable String inviteCode) {
        return ResponseEntity.ok(classroomService.resolveInvite(inviteCode));
    }

    @PostMapping("/api/student/identity")
    public ResponseEntity<StudentProfileResponse> bindStudentIdentity(@Valid @RequestBody StudentIdentityRequest request) {
        return ResponseEntity.ok(classroomService.bindStudentIdentity(request));
    }

    @GetMapping("/api/student/assignments/{assignmentId}/profile/{studentProfileId}/trajectory")
    public ResponseEntity<StudentTrajectoryResponse> getStudentTrajectory(@PathVariable Long assignmentId,
                                                                          @PathVariable Long studentProfileId) {
        return ResponseEntity.ok(studentTrajectoryService.buildTrajectory(assignmentId, studentProfileId));
    }

    @GetMapping("/api/student/profile/{studentProfileId}/ability-profile")
    public ResponseEntity<StudentAbilityProfileResponse> getStudentAbilityProfile(@PathVariable Long studentProfileId) {
        return ResponseEntity.ok(studentAbilityProfileService.buildProfile(studentProfileId));
    }

    @GetMapping("/api/student/profile/{studentProfileId}/recommendations")
    public ResponseEntity<StudentRecommendationResponse> getStudentRecommendations(@PathVariable Long studentProfileId) {
        return ResponseEntity.ok(studentRecommendationService.recommend(studentProfileId));
    }
}
