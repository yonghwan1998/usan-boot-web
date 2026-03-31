package com.usanmap.usan.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.usanmap.usan.service.storage.FileProperties;

@Configuration
@EnableConfigurationProperties(FileProperties.class)
public class FileConfig {
}
