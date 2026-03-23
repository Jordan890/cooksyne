package com.cartandcook.selfhosted.controller;

import com.cartandcook.selfhosted.contracts.RuntimeDbTestRequest;
import com.cartandcook.selfhosted.contracts.RuntimeDbTestResponse;
import com.cartandcook.selfhosted.contracts.RuntimeConfigRequest;
import com.cartandcook.selfhosted.contracts.RuntimeConfigResponse;
import com.cartandcook.selfhosted.service.RuntimeRestartService;
import com.cartandcook.selfhosted.service.RuntimeConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/config/runtime")
public class RuntimeConfigController {

    private final RuntimeConfigService runtimeConfigService;
    private final RuntimeRestartService runtimeRestartService;

    public RuntimeConfigController(
            RuntimeConfigService runtimeConfigService,
            RuntimeRestartService runtimeRestartService) {
        this.runtimeConfigService = runtimeConfigService;
        this.runtimeRestartService = runtimeRestartService;
    }

    @GetMapping
    public ResponseEntity<RuntimeConfigResponse> getConfig() {
        return ResponseEntity.ok(runtimeConfigService.get());
    }

    @PutMapping
    public ResponseEntity<RuntimeConfigResponse> saveConfig(@RequestBody RuntimeConfigRequest request) {
        RuntimeConfigResponse response = runtimeConfigService.save(request);
        if (response.isRestartRequired() && response.isAutoRestartOnConfigSave()) {
            runtimeRestartService.requestRestartAsync("Runtime config updated: " + response.getRestartRequiredKeys());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-db")
    public ResponseEntity<RuntimeDbTestResponse> testDb(@RequestBody RuntimeDbTestRequest request) {
        RuntimeDbTestResponse response = runtimeConfigService.testDbConnection(
                request.getDbUrl(),
                request.getDbUsername(),
                request.getDbPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rollback-db")
    public ResponseEntity<RuntimeConfigResponse> rollbackDb() {
        RuntimeConfigResponse response = runtimeConfigService.rollbackToLastKnownGoodDb();
        if (response.isRestartRequired() && response.isAutoRestartOnConfigSave()) {
            runtimeRestartService.requestRestartAsync("Runtime DB config rolled back to last known good");
        }
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
