package com.onlinejudge.system.api;

import com.onlinejudge.system.application.ExecutorStatusService;
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

    @GetMapping("/executor-status")
    public ResponseEntity<ExecutorStatusResponse> getExecutorStatus() {
        return ResponseEntity.ok(executorStatusService.getStatus());
    }
}
