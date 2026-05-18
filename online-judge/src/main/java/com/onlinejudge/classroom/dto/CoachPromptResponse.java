package com.onlinejudge.classroom.dto;

import com.onlinejudge.classroom.domain.CoachPrompt;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CoachPromptResponse {
    private Long id;
    private Long assignmentId;
    private Long studentProfileId;
    private Long submissionId;
    private Long parentPromptId;
    private Integer turnIndex;
    private String hintPolicy;
    private String promptType;
    private String question;
    private String studentAnswer;
    private String coachFeedback;
    private String rationale;
    private String contextSummary;
    private List<String> evidenceRefs;
    private List<CoachPromptResponse> turns;
    private LocalDateTime createdAt;

    public static CoachPromptResponse from(CoachPrompt prompt, List<String> evidenceRefs) {
        if (prompt == null) {
            return null;
        }
        return CoachPromptResponse.builder()
                .id(prompt.getId())
                .assignmentId(prompt.getAssignmentId())
                .studentProfileId(prompt.getStudentProfileId())
                .submissionId(prompt.getSubmissionId())
                .parentPromptId(prompt.getParentPromptId())
                .turnIndex(prompt.getTurnIndex())
                .hintPolicy(prompt.getHintPolicy())
                .promptType(prompt.getPromptType())
                .question(prompt.getQuestion())
                .studentAnswer(prompt.getStudentAnswer())
                .coachFeedback(prompt.getCoachFeedback())
                .rationale(prompt.getRationale())
                .contextSummary(prompt.getContextSummary())
                .evidenceRefs(evidenceRefs)
                .turns(List.of())
                .createdAt(prompt.getCreatedAt())
                .build();
    }

    public static CoachPromptResponse from(CoachPrompt prompt, List<String> evidenceRefs, List<CoachPromptResponse> turns) {
        CoachPromptResponse response = from(prompt, evidenceRefs);
        if (response != null) {
            response.setTurns(turns == null ? List.of() : turns);
        }
        return response;
    }
}
