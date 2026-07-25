package com.onlinejudge.problem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatementImportResponse {
    private String title;
    private String statementBackground;
    private String statementDescription;
    private String statementInputFormat;
    private String statementOutputFormat;
    private String statementSamples;
    private String statementHints;
}
