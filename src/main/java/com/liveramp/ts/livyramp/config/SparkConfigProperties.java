package com.liveramp.ts.livyramp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spark")
@Data
public class SparkConfigProperties {
    private String master;
    private Integer driverCores;
    private String driverMemory;
    private Integer executorCores;
    private String executorMemory;
    private Integer executorNum;
    private String deployMode;
}
