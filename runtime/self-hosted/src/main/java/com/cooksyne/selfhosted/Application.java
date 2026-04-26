package com.cooksyne.selfhosted;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.cooksyne.core",
        "com.cooksyne.adapters.persistencejpa",
        "com.cooksyne.selfhosted",
        "com.cooksyne.adapters.aiollama",
        "com.cooksyne.adapters.aiopenai",
        "com.cooksyne.adapters.aibedrock",
        "com.cooksyne.adapters.aihuggingface"
})
@EnableJpaRepositories(basePackages = "com.cooksyne.adapters.persistencejpa")
@EntityScan(basePackages = "com.cooksyne.adapters.persistencejpa")
@ConfigurationPropertiesScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}