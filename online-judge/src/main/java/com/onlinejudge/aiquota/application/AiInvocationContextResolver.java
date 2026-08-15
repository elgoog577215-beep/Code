package com.onlinejudge.aiquota.application;

import com.onlinejudge.classroom.persistence.AssignmentRepository;
import com.onlinejudge.submission.domain.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiInvocationContextResolver {
    private final AssignmentRepository assignments;

    public AiInvocationContext forSubmission(Submission submission, String purpose, String idempotencyKey) {
        if (submission == null || submission.getAssignmentId() == null) {
            return AiInvocationContext.anonymous(purpose, idempotencyKey);
        }
        UUID teacherId = assignments.findById(submission.getAssignmentId())
                .map(assignment -> assignment.getOwnerTeacherId())
                .orElse(null);
        return new AiInvocationContext(teacherId, submission.getStudentProfileId(), submission.getAssignmentId(),
                submission.getId(), purpose, idempotencyKey);
    }
}
