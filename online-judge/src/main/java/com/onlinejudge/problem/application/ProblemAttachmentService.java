package com.onlinejudge.problem.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.dto.ProblemAttachmentResponse;
import com.onlinejudge.problem.persistence.ProblemRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProblemAttachmentService {

    private static final long MAX_ATTACHMENT_BYTES = 50L * 1024L * 1024L;
    private static final TypeReference<List<AttachmentRecord>> ATTACHMENT_LIST = new TypeReference<>() {
    };

    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.problem-attachments.storage-root:data/problem-attachments}")
    private String storageRoot;

    public List<ProblemAttachmentResponse> list(Long problemId) {
        Problem problem = findProblem(problemId);
        return readRecords(problem).stream()
                .map(record -> toResponse(problemId, record))
                .toList();
    }

    @Transactional
    public ProblemAttachmentResponse upload(Long problemId, MultipartFile file) {
        Problem problem = findProblem(problemId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an attachment file.");
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("Attachment file exceeds 50 MB.");
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String id = Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path folder = root().resolve(String.valueOf(problemId)).resolve(id).normalize();
        Path target = folder.resolve(originalName).normalize();
        if (!target.startsWith(folder)) {
            throw new IllegalArgumentException("Unsafe attachment file name.");
        }

        try {
            Files.createDirectories(folder);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store attachment.", exception);
        }

        AttachmentRecord record = AttachmentRecord.builder()
                .id(id)
                .fileName(originalName)
                .relativePath(problemId + "/" + id + "/" + originalName)
                .sizeBytes(file.getSize())
                .contentType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType())
                .build();
        List<AttachmentRecord> records = new ArrayList<>(readRecords(problem));
        records.add(record);
        problem.setAttachments(writeRecords(records));
        problemRepository.save(problem);
        return toResponse(problemId, record);
    }

    public DownloadedAttachment download(Long problemId, String attachmentId) {
        Problem problem = findProblem(problemId);
        AttachmentRecord record = readRecords(problem).stream()
                .filter(item -> item.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attachment does not exist: " + attachmentId));
        Path path = resolve(record.getRelativePath());
        try {
            return DownloadedAttachment.builder()
                    .fileName(record.getFileName())
                    .contentType(record.getContentType())
                    .sizeBytes(Files.size(path))
                    .resource(new InputStreamResource(Files.newInputStream(path)))
                    .build();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read attachment.", exception);
        }
    }

    private Problem findProblem(Long problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Problem does not exist: " + problemId));
    }

    private List<AttachmentRecord> readRecords(Problem problem) {
        String value = problem.getAttachments();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            if (!value.stripLeading().startsWith("[")) {
                return List.of();
            }
            return objectMapper.readValue(value, ATTACHMENT_LIST);
        } catch (IOException exception) {
            return List.of();
        }
    }

    private String writeRecords(List<AttachmentRecord> records) {
        try {
            return objectMapper.writeValueAsString(records);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize attachments.", exception);
        }
    }

    private ProblemAttachmentResponse toResponse(Long problemId, AttachmentRecord record) {
        return ProblemAttachmentResponse.builder()
                .id(record.getId())
                .fileName(record.getFileName())
                .sizeBytes(record.getSizeBytes())
                .contentType(record.getContentType())
                .downloadUrl("/api/problems/" + problemId + "/attachments/" + record.getId() + "/download")
                .build();
    }

    private Path root() {
        return Path.of(storageRoot).toAbsolutePath().normalize();
    }

    private Path resolve(String relativePath) {
        Path resolved = root().resolve(relativePath).normalize();
        if (!resolved.startsWith(root())) {
            throw new IllegalArgumentException("Unsafe attachment path.");
        }
        return resolved;
    }

    private String sanitizeFileName(String fileName) {
        String value = fileName == null || fileName.isBlank() ? "attachment" : Path.of(fileName).getFileName().toString();
        value = value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return value.isBlank() ? "attachment" : value;
    }

    @Data
    @Builder
    private static class AttachmentRecord {
        private String id;
        private String fileName;
        private String relativePath;
        private Long sizeBytes;
        private String contentType;
    }

    @Data
    @Builder
    public static class DownloadedAttachment {
        private String fileName;
        private String contentType;
        private Long sizeBytes;
        private Resource resource;
    }
}
