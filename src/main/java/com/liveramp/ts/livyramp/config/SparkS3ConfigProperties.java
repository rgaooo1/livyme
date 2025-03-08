package com.liveramp.ts.livyramp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spark.hadoop.fs.s3a")
public class SparkS3ConfigProperties {
    private String impl;
    private String endpoint;
    private String accessKey;
    private String secretKey;
}
