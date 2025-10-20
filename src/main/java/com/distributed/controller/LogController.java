package com.distributed.controller;

import com.distributed.model.ServerLog;
import com.distributed.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/sendRequestsAndSave")
    public ResponseEntity<String> fetchAndSaveLogs(@RequestParam int count) {
        logService.fetchAndSaveLogs(count);
        return ResponseEntity.ok("Successfully sent " + count + " requests.");
    }

    @GetMapping("/error")
    public List<ServerLog> getErrorLogs() {
        return logService.getErrorLogs();
    }

    @GetMapping("/success")
    public List<ServerLog> getSuccessfulLogs() {
        return logService.getSuccessfulLogs();
    }

    @GetMapping("/error-distribution")
    public Map<String, Object> getErrorDistribution() {
        return logService.getErrorDistribution();
    }





}