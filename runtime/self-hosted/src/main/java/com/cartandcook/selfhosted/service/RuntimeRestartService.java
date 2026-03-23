package com.cartandcook.selfhosted.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class RuntimeRestartService {

    private static final Logger log = LoggerFactory.getLogger(RuntimeRestartService.class);

    private final ConfigurableApplicationContext applicationContext;

    @Value("${cartandcook.runtime.auto-restart-delay-ms:1500}")
    private long autoRestartDelayMs;

    public RuntimeRestartService(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void requestRestartAsync(String reason) {
        Thread restartThread = new Thread(() -> {
            try {
                log.warn("Runtime restart requested: {}. Shutting down in {} ms", reason, autoRestartDelayMs);
                Thread.sleep(Math.max(0, autoRestartDelayMs));
                int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exitCode);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Runtime restart interrupted", ie);
            } catch (Exception ex) {
                log.error("Failed to restart runtime", ex);
            }
        }, "runtime-config-restart-thread");
        restartThread.setDaemon(false);
        restartThread.start();
    }
}
