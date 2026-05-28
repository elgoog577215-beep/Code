package com.onlinejudge.system.api;

import com.onlinejudge.system.application.AiRouteHealthService;
import com.onlinejudge.system.application.ExecutorStatusService;
import com.onlinejudge.system.dto.AiRouteHealthResponse;
import com.onlinejudge.system.dto.ExecutorStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final ExecutorStatusService executorStatusService;
    private final AiRouteHealthService aiRouteHealthService;

    @GetMapping("/executor-status")
    public ResponseEntity<ExecutorStatusResponse> getExecutorStatus() {
        return ResponseEntity.ok(executorStatusService.getStatus());
    }

    @GetMapping("/ai-route-health")
    public ResponseEntity<AiRouteHealthResponse> getAiRouteHealth() {
        return ResponseEntity.ok(aiRouteHealthService.getHealth());
    }
}
