package com.onlinejudge.problem.application;

import com.onlinejudge.problem.domain.TestCase;
import com.onlinejudge.problem.dto.TestDataFilePreviewResponse;
import com.onlinejudge.problem.dto.TestDataImportCommitResponse;
import com.onlinejudge.problem.dto.TestDataImportPreviewResponse;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.problem.persistence.TestCaseRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ProblemTestDataImportService {

    private static final long MAX_ZIP_BYTES = 50L * 1024L * 1024L;
    private static final long MAX_UNCOMPRESSED_BYTES = 100L * 1024L * 1024L;
    private static final int MAX_PAIRS = 50;
    private static final int DEFAULT_TIME_LIMIT_MS = 1000;
    private static final int DEFAULT_MEMORY_LIMIT_KIB = 128 * 1024;
    private static final Pattern DIGIT_GROUP = Pattern.compile("\\d+");

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestCaseContentService contentService;

    @Value("${app.problem-test-data.storage-root:data/problem-test-data}")
    private String storageRoot;

    public TestDataImportPreviewResponse preview(Long problemId, MultipartFile file) {
        problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Problem does not exist: " + problemId));
        return parse(file, false).toResponse();
    }

    @Transactional
    public TestDataImportCommitResponse commit(Long problemId, MultipartFile file) {
        problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Problem does not exist: " + problemId));
        ParsedArchive archive = parse(file, true);
        if (!archive.issues.isEmpty()) {
            throw new IllegalArgumentException("Zip data is invalid; preview must pass before commit.");
        }

        String batchId = Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path batchDir = Path.of(storageRoot).toAbsolutePath().normalize()
                .resolve(String.valueOf(problemId))
                .resolve(batchId)
                .normalize();
        try {
            Files.createDirectories(batchDir);
            for (Pair pair : archive.pairs) {
                Files.write(batchDir.resolve(pair.inputName), pair.inputBytes);
                Files.write(batchDir.resolve(pair.outputName), pair.outputBytes);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store test data package", exception);
        }

        testCaseRepository.deleteByProblemId(problemId);
        List<Long> ids = new ArrayList<>();
        for (int index = 0; index < archive.pairs.size(); index++) {
            Pair pair = archive.pairs.get(index);
            TestCase saved = testCaseRepository.save(TestCase.builder()
                    .problemId(problemId)
                    .input("")
                    .expectedOutput("")
                    .inputStorageType(TestCase.StorageType.FILE)
                    .outputStorageType(TestCase.StorageType.FILE)
                    .inputFilePath(problemId + "/" + batchId + "/" + pair.inputName)
                    .outputFilePath(problemId + "/" + batchId + "/" + pair.outputName)
                    .inputFileName(pair.inputName)
                    .outputFileName(pair.outputName)
                    .inputSizeBytes((long) pair.inputBytes.length)
                    .outputSizeBytes((long) pair.outputBytes.length)
                    .inputSha256(pair.inputSha256)
                    .outputSha256(pair.outputSha256)
                    .timeLimitMs(DEFAULT_TIME_LIMIT_MS)
                    .memoryLimitKib(DEFAULT_MEMORY_LIMIT_KIB)
                    .subtaskIndex(0)
                    .score(0)
                    .publicExample(false)
                    .importBatchId(batchId)
                    .isHidden(true)
                    .orderIndex(index)
                    .build());
            ids.add(saved.getId());
        }

        return TestDataImportCommitResponse.builder()
                .importBatchId(batchId)
                .testCaseCount(ids.size())
                .uncompressedBytes(archive.uncompressedBytes)
                .testCaseIds(ids)
                .preview(archive.toResponse())
                .build();
    }

    public TestDataFilePreviewResponse previewStoredFile(Long problemId, Long testCaseId, String kind) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .filter(item -> item.getProblemId().equals(problemId))
                .orElseThrow(() -> new IllegalArgumentException("Test case does not exist: " + testCaseId));
        boolean output = "output".equalsIgnoreCase(kind) || "out".equalsIgnoreCase(kind);
        String fileName = output ? testCase.getOutputFileName() : testCase.getInputFileName();
        String relativePath = output ? testCase.getOutputFilePath() : testCase.getInputFilePath();
        long sizeBytes = output
                ? nullToZero(testCase.getOutputSizeBytes())
                : nullToZero(testCase.getInputSizeBytes());
        Path path = contentService.resolveStoredPath(relativePath);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<String> previewLines = new ArrayList<>();
            String line;
            while (previewLines.size() < 10 && (line = reader.readLine()) != null) {
                previewLines.add(line.length() > 240 ? line.substring(0, 240) + "..." : line);
            }
            boolean truncated = reader.readLine() != null;
            return TestDataFilePreviewResponse.builder()
                    .testCaseId(testCaseId)
                    .kind(output ? "output" : "input")
                    .fileName(fileName)
                    .sizeBytes(sizeBytes)
                    .lines(previewLines)
                    .truncated(truncated)
                    .build();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to preview stored test data", exception);
        }
    }

    public byte[] downloadPackage(Long problemId) {
        var problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Problem does not exist: " + problemId));
        if (!Boolean.TRUE.equals(problem.getDataDownloadEnabled())) {
            throw new IllegalStateException("Data download is disabled for this problem.");
        }
        List<TestCase> cases = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId)
                .stream()
                .filter(item -> item.getInputStorageType() == TestCase.StorageType.FILE)
                .toList();
        try (var output = new java.io.ByteArrayOutputStream();
             var zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (TestCase testCase : cases) {
                writeZipEntry(zip, testCase.getInputFileName(), contentService.resolveStoredPath(testCase.getInputFilePath()));
                writeZipEntry(zip, testCase.getOutputFileName(), contentService.resolveStoredPath(testCase.getOutputFilePath()));
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to build test data package", exception);
        }
    }

    private ParsedArchive parse(MultipartFile file, boolean keepBytes) {
        List<TestDataImportPreviewResponse.Issue> issues = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            issues.add(issue("error", "Please choose a zip file.", ""));
            return ParsedArchive.builder().issues(issues).pairs(List.of()).build();
        }
        String originalFileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!originalFileName.toLowerCase().endsWith(".zip")) {
            issues.add(issue("error", "Only .zip files are supported.", originalFileName));
        }
        if (file.getSize() > MAX_ZIP_BYTES) {
            issues.add(issue("error", "Zip file exceeds 50 MB.", originalFileName));
        }

        Map<String, EntryFile> inputs = new HashMap<>();
        Map<String, EntryFile> outputs = new HashMap<>();
        long uncompressed = 0L;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file.getBytes()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!isSafeRootFile(name) || entry.isDirectory()) {
                    issues.add(issue("error", "Zip must contain root-level files only.", name));
                    continue;
                }
                String lowerName = name.toLowerCase();
                if (!lowerName.endsWith(".in") && !lowerName.endsWith(".out")) {
                    issues.add(issue("error", "Only .in and .out files are allowed.", name));
                    continue;
                }
                byte[] bytes = zip.readAllBytes();
                uncompressed += bytes.length;
                if (uncompressed > MAX_UNCOMPRESSED_BYTES) {
                    issues.add(issue("error", "Uncompressed data exceeds 100 MB.", name));
                    break;
                }
                String base = name.substring(0, name.lastIndexOf('.'));
                Integer number = extractNumber(base);
                if (number == null) {
                    issues.add(issue("error", "File name must contain exactly one continuous digit group.", name));
                    continue;
                }
                EntryFile entryFile = EntryFile.builder()
                        .name(name)
                        .base(base)
                        .number(number)
                        .bytes(keepBytes ? bytes : bytes)
                        .sha256(sha256(bytes))
                        .build();
                Map<String, EntryFile> target = lowerName.endsWith(".in") ? inputs : outputs;
                if (target.put(base, entryFile) != null) {
                    issues.add(issue("error", "Duplicate test data file basename.", name));
                }
            }
        } catch (IOException exception) {
            issues.add(issue("error", "Unable to read zip: " + exception.getMessage(), originalFileName));
        }

        List<Pair> pairs = new ArrayList<>();
        for (Map.Entry<String, EntryFile> entry : inputs.entrySet()) {
            EntryFile output = outputs.get(entry.getKey());
            if (output == null) {
                issues.add(issue("error", "Missing .out pair.", entry.getValue().name));
                continue;
            }
            pairs.add(Pair.builder()
                    .number(entry.getValue().number)
                    .inputName(entry.getValue().name)
                    .outputName(output.name)
                    .inputBytes(entry.getValue().bytes)
                    .outputBytes(output.bytes)
                    .inputSha256(entry.getValue().sha256)
                    .outputSha256(output.sha256)
                    .build());
        }
        for (Map.Entry<String, EntryFile> entry : outputs.entrySet()) {
            if (!inputs.containsKey(entry.getKey())) {
                issues.add(issue("error", "Missing .in pair.", entry.getValue().name));
            }
        }
        pairs.sort(Comparator.comparingInt(Pair::getNumber).thenComparing(Pair::getInputName));
        if (pairs.size() > MAX_PAIRS) {
            issues.add(issue("error", "Test point count exceeds 50 pairs.", ""));
        }

        return ParsedArchive.builder()
                .compressedBytes(file.getSize())
                .uncompressedBytes(uncompressed)
                .pairs(pairs)
                .issues(issues)
                .build();
    }

    private boolean isSafeRootFile(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return !name.contains("/")
                && !name.contains("\\")
                && !name.contains("..")
                && !name.startsWith("/")
                && !name.matches("^[A-Za-z]:.*");
    }

    private Integer extractNumber(String base) {
        Matcher matcher = DIGIT_GROUP.matcher(base);
        Integer value = null;
        int count = 0;
        while (matcher.find()) {
            count++;
            if (count == 1) {
                value = Integer.parseInt(matcher.group());
            }
        }
        return count == 1 ? value : null;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private void writeZipEntry(ZipOutputStream zip, String fileName, Path path) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        zip.putNextEntry(new ZipEntry(fileName));
        Files.copy(path, zip);
        zip.closeEntry();
    }

    private TestDataImportPreviewResponse.Issue issue(String severity, String message, String fileName) {
        return TestDataImportPreviewResponse.Issue.builder()
                .severity(severity)
                .message(message)
                .fileName(fileName)
                .build();
    }

    @Data
    @Builder
    private static class EntryFile {
        private String name;
        private String base;
        private int number;
        private byte[] bytes;
        private String sha256;
    }

    @Data
    @Builder
    private static class Pair {
        private int number;
        private String inputName;
        private String outputName;
        private byte[] inputBytes;
        private byte[] outputBytes;
        private String inputSha256;
        private String outputSha256;
    }

    @Data
    @Builder
    private static class ParsedArchive {
        @Builder.Default
        private long compressedBytes = 0;
        @Builder.Default
        private long uncompressedBytes = 0;
        @Builder.Default
        private List<Pair> pairs = List.of();
        @Builder.Default
        private List<TestDataImportPreviewResponse.Issue> issues = List.of();

        TestDataImportPreviewResponse toResponse() {
            List<TestDataImportPreviewResponse.Row> rows = new ArrayList<>();
            for (int index = 0; index < pairs.size(); index++) {
                Pair pair = pairs.get(index);
                rows.add(TestDataImportPreviewResponse.Row.builder()
                        .displayIndex(index + 1)
                        .number(pair.number)
                        .inputFileName(pair.inputName)
                        .outputFileName(pair.outputName)
                        .inputSizeBytes(pair.inputBytes == null ? 0 : pair.inputBytes.length)
                        .outputSizeBytes(pair.outputBytes == null ? 0 : pair.outputBytes.length)
                        .inputSha256(pair.inputSha256)
                        .outputSha256(pair.outputSha256)
                        .build());
            }
            return TestDataImportPreviewResponse.builder()
                    .valid(issues.isEmpty())
                    .message(issues.isEmpty() ? "Zip data parsed successfully." : "Zip data has validation issues.")
                    .compressedBytes(compressedBytes)
                    .uncompressedBytes(uncompressedBytes)
                    .pairCount(pairs.size())
                    .rows(rows)
                    .issues(issues)
                    .build();
        }
    }
}
