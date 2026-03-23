package com.cartandcook.selfhosted.controller;

import com.cartandcook.selfhosted.contracts.RuntimeConfigRequest;
import com.cartandcook.selfhosted.contracts.RuntimeConfigResponse;
import com.cartandcook.selfhosted.service.RuntimeRestartService;
import com.cartandcook.selfhosted.service.RuntimeConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
