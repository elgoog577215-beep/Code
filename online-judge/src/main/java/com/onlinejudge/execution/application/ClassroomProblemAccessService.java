package com.onlinejudge.execution.application;

import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.classroom.domain.StudentProfile;
import com.onlinejudge.classroom.persistence.AssignmentRepository;
import com.onlinejudge.classroom.persistence.AssignmentTaskRepository;
import com.onlinejudge.classroom.persistence.StudentProfileRepository;
import com.onlinejudge.shared.security.AccessDeniedException;
import com.onlinejudge.shared.security.AuthenticationRequiredException;
import com.onlinejudge.shared.security.StudentAccessTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClassroomProblemAccessService {

    private final StudentAccessTokenService studentAccessTokenService;
    private final StudentProfileRepository studentProfileRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentTaskRepository assignmentTaskRepository;

    public Long requireAccess(Long assignmentId, Long problemId, HttpServletRequest request) {
        Long studentId = studentAccessTokenService.currentStudentId(request);
        if (assignmentId == null) {
            return studentId;
        }
        if (studentId == null) {
            throw new AuthenticationRequiredException("课堂作业运行代码前请先登录学生端");
        }

        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new AuthenticationRequiredException("学生身份不存在，请重新登录"));
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("作业不存在: " + assignmentId));
        if (assignment.getClassGroupId() == null
                || student.getClassGroupId() == null
                || !Objects.equals(assignment.getClassGroupId(), student.getClassGroupId())) {
            throw new AccessDeniedException("当前学生不属于该作业班级");
        }
        if (!assignmentTaskRepository.existsByAssignmentIdAndProblemId(assignmentId, problemId)) {
            throw new AccessDeniedException("该题目不属于当前作业");
        }
        return studentId;
    }
}
