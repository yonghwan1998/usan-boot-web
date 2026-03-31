package com.usanmap.usan.service.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "file")
public class FileProperties {
    private String uploadDir = "uploads";
    private String publicPrefix = "/uploads";
}
