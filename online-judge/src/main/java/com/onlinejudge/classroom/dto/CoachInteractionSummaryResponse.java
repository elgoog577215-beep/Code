package com.onlinejudge.classroom.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CoachInteractionSummaryResponse {
    private Long submissionId;
    private int turnCount;
    private int answeredTurnCount;
    private boolean prompted;
    private boolean answered;
    private String status;
    private String statusLabel;
    private String summary;
    private String latestQuestion;
    private String latestAnswer;
    private String latestFeedback;
    private LocalDateTime latestAt;
    private CoachImpactResponse impact;
}
