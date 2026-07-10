package com.onlinejudge.submission.application;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.submission-evidence")
public class SubmissionEvidenceProperties {
    private boolean strictClassroomContextEnabled = true;
    private boolean analyticsReadEnabled = true;
}
