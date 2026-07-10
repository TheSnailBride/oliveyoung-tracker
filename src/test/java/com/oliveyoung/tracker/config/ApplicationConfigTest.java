package com.oliveyoung.tracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigTest {

    @Test
    @DisplayName("메인 application.yml은 로컬 .env 파일을 읽는다")
    void mainApplicationConfigImportsLocalDotenv() throws IOException {
        var resource = new FileSystemResource("src/main/resources/application.yml");
        var propertySources = new YamlPropertySourceLoader().load("application", resource);

        assertThat(propertySources)
                .extracting(propertySource -> propertySource.getProperty("spring.config.import"))
                .contains("optional:file:.env[.properties]");
    }
}
