package com.onlinejudge.submission.application;

import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.classroom.domain.StudentProfile;
import com.onlinejudge.classroom.persistence.AssignmentRepository;
import com.onlinejudge.classroom.persistence.AssignmentTaskRepository;
import com.onlinejudge.classroom.persistence.StudentProfileRepository;
import com.onlinejudge.shared.security.AccessDeniedException;
import com.onlinejudge.shared.security.AuthenticationRequiredException;
import com.onlinejudge.shared.security.StudentAccessTokenService;
import com.onlinejudge.submission.dto.SubmissionRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomSubmissionContextService {

    private final StudentAccessTokenService studentAccessTokenService;
    private final StudentProfileRepository studentProfileRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentTaskRepository assignmentTaskRepository;
    private final SubmissionEvidenceProperties properties;

    public SubmissionRequest resolve(SubmissionRequest request, HttpServletRequest httpRequest) {
        Long tokenStudentId = studentAccessTokenService.currentStudentId(httpRequest);
        Long requestedStudentId = request.getStudentProfileId();

        if (request.getAssignmentId() == null) {
            resolvePublicIdentity(request, httpRequest, tokenStudentId, requestedStudentId);
            return request;
        }

        if (tokenStudentId == null) {
            if (requestedStudentId != null) {
                studentAccessTokenService.requireStudent(httpRequest, requestedStudentId);
            }
            if (properties.isStrictClassroomContextEnabled()) {
                reject("MISSING_STUDENT_TOKEN", request);
                throw new AuthenticationRequiredException("课堂作业提交前请先登录学生端");
            }
            return request;
        }
        if (requestedStudentId != null && !Objects.equals(requestedStudentId, tokenStudentId)) {
            reject("STUDENT_ID_MISMATCH", request);
            throw new AccessDeniedException("提交学生与当前登录身份不一致");
        }

        StudentProfile student = studentProfileRepository.findById(tokenStudentId)
                .orElseThrow(() -> new AuthenticationRequiredException("学生身份不存在，请重新登录"));
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("作业不存在: " + request.getAssignmentId()));
        if (assignment.getClassGroupId() == null
                || student.getClassGroupId() == null
                || !Objects.equals(assignment.getClassGroupId(), student.getClassGroupId())) {
            reject("CLASS_ASSIGNMENT_MISMATCH", request);
            throw new AccessDeniedException("当前学生不属于该作业班级");
        }
        if (!assignmentTaskRepository.existsByAssignmentIdAndProblemId(request.getAssignmentId(), request.getProblemId())) {
            reject("PROBLEM_NOT_IN_ASSIGNMENT", request);
            throw new IllegalArgumentException("该题目不属于当前作业");
        }
        request.setStudentProfileId(tokenStudentId);
        return request;
    }

    private void resolvePublicIdentity(SubmissionRequest request,
                                       HttpServletRequest httpRequest,
                                       Long tokenStudentId,
                                       Long requestedStudentId) {
        if (tokenStudentId == null) {
            if (requestedStudentId != null) {
                studentAccessTokenService.requireStudent(httpRequest, requestedStudentId);
            }
            request.setStudentProfileId(null);
            return;
        }
        if (requestedStudentId != null && !Objects.equals(requestedStudentId, tokenStudentId)) {
            reject("PUBLIC_STUDENT_ID_MISMATCH", request);
            throw new AccessDeniedException("提交学生与当前登录身份不一致");
        }
        request.setStudentProfileId(tokenStudentId);
    }

    private void reject(String reason, SubmissionRequest request) {
        log.warn("Rejected submission context. reason={}, assignmentId={}, problemId={}, requestedStudentId={}",
                reason, request.getAssignmentId(), request.getProblemId(), request.getStudentProfileId());
    }
}
