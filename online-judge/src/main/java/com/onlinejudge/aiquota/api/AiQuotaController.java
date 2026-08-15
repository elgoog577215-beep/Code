package com.onlinejudge.aiquota.api;

import com.onlinejudge.aiquota.application.AiQuotaService;
import com.onlinejudge.aiquota.dto.AdjustTeacherQuotaRequest;
import com.onlinejudge.aiquota.dto.TeacherAiUsageResponse;
import com.onlinejudge.identity.application.AuditService;
import com.onlinejudge.identity.application.CurrentTeacherContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AiQuotaController {
    private final AiQuotaService quotas;
    private final AuditService audit;
    private final CurrentTeacherContext currentTeacher;

    @GetMapping("/api/teacher/usage/current")
    public ResponseEntity<TeacherAiUsageResponse> current() {
        return ResponseEntity.ok(quotas.currentUsage());
    }

    @PutMapping("/api/admin/teachers/{teacherId}/quota")
    public ResponseEntity<TeacherAiUsageResponse> adjust(@PathVariable UUID teacherId,
                                                         @Valid @RequestBody AdjustTeacherQuotaRequest request) {
        TeacherAiUsageResponse response = quotas.adjust(teacherId, request.baseUnits(), request.additionalUnits());
        audit.record(currentTeacher.requireAdmin().id(), "TEACHER_QUOTA_ADJUSTED", "TEACHER", teacherId,
                "baseUnits=" + request.baseUnits() + ", additionalUnits=" + request.additionalUnits(), null);
        return ResponseEntity.ok(response);
    }
}
