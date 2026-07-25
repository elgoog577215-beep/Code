package com.onlinejudge.problem.application;

import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.dto.TestDataImportPreviewResponse;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.problem.persistence.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProblemTestDataImportServiceTest {

    private ProblemRepository problemRepository;
    private ProblemTestDataImportService service;

    @BeforeEach
    void setUp() {
        problemRepository = mock(ProblemRepository.class);
        TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);
        TestCaseContentService contentService = mock(TestCaseContentService.class);
        service = new ProblemTestDataImportService(problemRepository, testCaseRepository, contentService);
        when(problemRepository.findById(1L)).thenReturn(Optional.of(Problem.builder().id(1L).build()));
    }

    @Test
    void previewsValidFlatPairsSortedByNumber() throws IOException {
        TestDataImportPreviewResponse response = service.preview(1L, zip(
                entry("game002.in", "2 3\n"),
                entry("game002.out", "5\n"),
                entry("game001.in", "1 2\n"),
                entry("game001.out", "3\n")
        ));

        assertThat(response.isValid()).isTrue();
        assertThat(response.getPairCount()).isEqualTo(2);
        assertThat(response.getRows()).extracting(TestDataImportPreviewResponse.Row::getDisplayIndex)
                .containsExactly(1, 2);
        assertThat(response.getRows()).extracting(TestDataImportPreviewResponse.Row::getInputFileName)
                .containsExactly("game001.in", "game002.in");
    }

    @Test
    void rejectsNestedFoldersExtraFilesMissingPairsAndBadDigitGroups() throws IOException {
        TestDataImportPreviewResponse response = service.preview(1L, zip(
                entry("folder/game001.in", "1\n"),
                entry("readme.txt", "ignored\n"),
                entry("game002.in", "2\n"),
                entry("T1-1.in", "3\n"),
                entry("T1-1.out", "3\n")
        ));

        assertThat(response.isValid()).isFalse();
        assertThat(response.getIssues()).extracting(TestDataImportPreviewResponse.Issue::getMessage)
                .anyMatch(message -> message.contains("root-level"))
                .anyMatch(message -> message.contains(".in and .out"))
                .anyMatch(message -> message.contains("Missing .out"))
                .anyMatch(message -> message.contains("exactly one continuous digit group"));
    }

    @Test
    void rejectsMoreThanFiftyPairs() throws IOException {
        ZipFileEntry[] entries = new ZipFileEntry[102];
        for (int index = 0; index < 51; index++) {
            entries[index * 2] = entry("case" + index + ".in", index + "\n");
            entries[index * 2 + 1] = entry("case" + index + ".out", index + "\n");
        }

        TestDataImportPreviewResponse response = service.preview(1L, zip(entries));

        assertThat(response.isValid()).isFalse();
        assertThat(response.getIssues()).extracting(TestDataImportPreviewResponse.Issue::getMessage)
                .anyMatch(message -> message.contains("exceeds 50 pairs"));
    }

    private MockMultipartFile zip(ZipFileEntry... entries) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (ZipFileEntry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name()));
                zip.write(entry.content().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return new MockMultipartFile("file", "data.zip", "application/zip", output.toByteArray());
    }

    private ZipFileEntry entry(String name, String content) {
        return new ZipFileEntry(name, content);
    }

    private record ZipFileEntry(String name, String content) {
    }
}
