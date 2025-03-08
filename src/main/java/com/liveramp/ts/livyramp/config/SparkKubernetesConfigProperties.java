package com.liveramp.ts.livyramp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spark.kubernetes")
public class SparkKubernetesConfigProperties {
    private String containerImage;
    private String containerImagePullPolicy;
    private String fileUploadPath;
    private String authenticateDriverServiceAccountName;
    private String namespace;
    private String imagePullSecret;

    private String driverVolumesNfsCfsOptionsServer;
    private String driverVolumesNfsCfsOptionsPath;
    private String driverVolumesNfsCfsMountPath;
    private String driverVolumesNfsCfsMountReadOnly;
    private String executorVolumesNfsCfsOptionsServer;
    private String executorVolumesNfsCfsOptionsPath;
    private String executorVolumesNfsCfsMountPath;
    private String executorVolumesNfsCfsMountReadOnly;
}
