package com.onlinejudge.problem.application;

import com.onlinejudge.problem.domain.TestCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class TestCaseContentService {

    @Value("${app.problem-test-data.storage-root:data/problem-test-data}")
    private String storageRoot;

    public String input(TestCase testCase) {
        if (testCase == null) {
            return "";
        }
        if (testCase.getInputStorageType() == TestCase.StorageType.FILE) {
            return readStoredText(testCase.getInputFilePath());
        }
        return testCase.getInput() == null ? "" : testCase.getInput();
    }

    public String expectedOutput(TestCase testCase) {
        if (testCase == null) {
            return "";
        }
        if (testCase.getOutputStorageType() == TestCase.StorageType.FILE) {
            return readStoredText(testCase.getOutputFilePath());
        }
        return testCase.getExpectedOutput() == null ? "" : testCase.getExpectedOutput();
    }

    public String previewInput(TestCase testCase, int maxChars) {
        return limit(input(testCase), maxChars);
    }

    public String previewExpectedOutput(TestCase testCase, int maxChars) {
        return limit(expectedOutput(testCase), maxChars);
    }

    public Path resolveStoredPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("File path is empty");
        }
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Unsafe stored file path");
        }
        return resolved;
    }

    private String readStoredText(String relativePath) {
        try {
            return Files.readString(resolveStoredPath(relativePath), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read stored test data", exception);
        }
    }

    private String limit(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxChars);
    }
}
