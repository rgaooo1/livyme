package com.liveramp.ts.livyramp.entity;

import lombok.Data;
import java.util.List;
import java.util.Map;
@Data
public class JobRequest {
    private String name;
    private String file;
    private String className;
    private List<String> args;
    private List<String> jars;

    private Map<String, String> conf;
    private String DeployMode = "cluster";
    private String master = "k8s://https://kubernetes.default.svc";
    private Integer driverCores = 1;
    private String driverMemory = "500m";
    private Integer executorCores = 1;
    private String executorMemory = "500m";
    private Integer numExecutors = 1;
}