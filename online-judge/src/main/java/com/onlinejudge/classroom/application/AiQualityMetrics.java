package com.onlinejudge.classroom.application;

import com.onlinejudge.classroom.domain.TeacherDiagnosisCorrection;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.submission.domain.SubmissionAnalysis;

import java.util.List;

class AiQualityMetrics {

    static final double LOW_CONFIDENCE_THRESHOLD = 0.6;

    private final long analyzedSubmissionCount;
    private final long correctionCount;
    private final long evalCandidateCount;
    private final long lowConfidenceCount;
    private final long highLeakRiskCount;

    private AiQualityMetrics(long analyzedSubmissionCount,
                             long correctionCount,
                             long evalCandidateCount,
                             long lowConfidenceCount,
                             long highLeakRiskCount) {
        this.analyzedSubmissionCount = analyzedSubmissionCount;
        this.correctionCount = correctionCount;
        this.evalCandidateCount = evalCandidateCount;
        this.lowConfidenceCount = lowConfidenceCount;
        this.highLeakRiskCount = highLeakRiskCount;
    }

    static AiQualityMetrics from(List<SubmissionAnalysis> analyses,
                                 List<TeacherDiagnosisCorrection> corrections,
                                 DiagnosisReportReader diagnosisReportReader) {
        List<SubmissionAnalysis> safeAnalyses = analyses == null ? List.of() : analyses;
        List<TeacherDiagnosisCorrection> safeCorrections = corrections == null ? List.of() : corrections;
        long lowConfidenceCount = safeAnalyses.stream()
                .filter(analysis -> {
                    Double confidence = diagnosisReportReader.confidence(analysis);
                    return confidence == null || confidence < LOW_CONFIDENCE_THRESHOLD;
                })
                .count();
        long highLeakRiskCount = safeAnalyses.stream()
                .filter(analysis -> "HIGH".equalsIgnoreCase(diagnosisReportReader.answerLeakRisk(analysis)))
                .count();
        return new AiQualityMetrics(
                safeAnalyses.size(),
                safeCorrections.size(),
                safeCorrections.stream().filter(TeacherDiagnosisCorrection::isEvalCandidate).count(),
                lowConfidenceCount,
                highLeakRiskCount
        );
    }

    long analyzedSubmissionCount() {
        return analyzedSubmissionCount;
    }

    long correctionCount() {
        return correctionCount;
    }

    long evalCandidateCount() {
        return evalCandidateCount;
    }

    long lowConfidenceCount() {
        return lowConfidenceCount;
    }

    long highLeakRiskCount() {
        return highLeakRiskCount;
    }

    double correctionRate() {
        return rate(correctionCount, analyzedSubmissionCount);
    }

    double lowConfidenceRate() {
        return rate(lowConfidenceCount, analyzedSubmissionCount);
    }

    double highLeakRiskRate() {
        return rate(highLeakRiskCount, analyzedSubmissionCount);
    }

    static double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.round((numerator * 1000.0 / denominator)) / 10.0;
    }
}
