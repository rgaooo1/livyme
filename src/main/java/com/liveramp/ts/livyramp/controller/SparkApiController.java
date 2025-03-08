package com.liveramp.ts.livyramp.controller;

import com.alibaba.fastjson.JSON;
import com.liveramp.ts.livyramp.config.Consts;
import com.liveramp.ts.livyramp.config.SparkConfigProperties;
import com.liveramp.ts.livyramp.config.SparkKubernetesConfigProperties;
import com.liveramp.ts.livyramp.config.SparkS3ConfigProperties;
import com.liveramp.ts.livyramp.entity.Job;
import com.liveramp.ts.livyramp.entity.JobHistory;
import com.liveramp.ts.livyramp.entity.SparkSubmitParam;
import com.liveramp.ts.livyramp.service.JobHistoryService;
import com.liveramp.ts.livyramp.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v1")
public class SparkApiController {
    private final SparkConfigProperties sparkConfig;
    private final SparkS3ConfigProperties s3Config;
    private final SparkKubernetesConfigProperties k8sConfig;
    private final JobService jobService;
    private final JobHistoryService jobHistoryService;

    public SparkApiController(SparkConfigProperties sparkConfig, SparkS3ConfigProperties s3Config, SparkKubernetesConfigProperties k8sConfig, JobService jobService, JobHistoryService jobHistoryService) {
        this.sparkConfig = sparkConfig;
        this.s3Config = s3Config;
        this.k8sConfig = k8sConfig;
        this.jobService = jobService;
        this.jobHistoryService = jobHistoryService;
    }

    @PostMapping(value = "/submit")
    public Object Submit(@RequestBody SparkSubmitParam sparkSubmitParam) throws Exception {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String name = sparkSubmitParam.getAppName().replaceAll("[\u4e00-\u9fa5]+|[\\pP\\pS\\pZ]", "").toLowerCase();
        String driverPodName = String.format("%s-%s-driver", name, uuid);
        String executorPodNamePrefix = String.format("%s-%s", name, uuid);

        if (driverPodName.length() >= 47) {
            driverPodName = driverPodName.substring(0, 46) + "-driver";
        }

        if (executorPodNamePrefix.length() >= 47) {
            executorPodNamePrefix = executorPodNamePrefix.substring(0, 46);
        }

        /**** build 参数 ****/
        Map<String, String> params = new HashMap<>();
        // 1. 默认配置

        params.put("spark.driver.cores", String.valueOf(sparkConfig.getDriverCores()));
        params.put(SparkLauncher.DRIVER_MEMORY, sparkConfig.getDriverMemory());
        params.put(SparkLauncher.EXECUTOR_CORES, String.valueOf(sparkConfig.getExecutorCores()));
        params.put(SparkLauncher.EXECUTOR_MEMORY, sparkConfig.getExecutorMemory());
        params.put(SparkLauncher.DEPLOY_MODE, sparkConfig.getDeployMode());
        params.put("spark.executor.instances", String.valueOf(sparkConfig.getExecutorNum()));

        params.put("spark.hadoop.fs.s3a.impl", s3Config.getImpl());
        params.put("spark.hadoop.fs.s3a.endpoint", s3Config.getEndpoint());
        params.put("spark.hadoop.fs.s3a.access.key", s3Config.getAccessKey());
        params.put("spark.hadoop.fs.s3a.secret.key", s3Config.getSecretKey());

        params.put("spark.kubernetes.container.image", k8sConfig.getContainerImage());
        params.put("spark.kubernetes.container.image.pullPolicy", k8sConfig.getContainerImagePullPolicy());

        params.put("spark.kubernetes.namespace", k8sConfig.getNamespace());
        params.put("spark.kubernetes.authenticate.driver.serviceAccountName", k8sConfig.getAuthenticateDriverServiceAccountName());
        params.put("spark.kubernetes.file.upload.path", k8sConfig.getFileUploadPath());
        if (Strings.isNotBlank(k8sConfig.getImagePullSecret())) {
            log.info("spark.kubernetes.container.image.pullSecrets:{}", k8sConfig.getImagePullSecret());
            params.put("spark.kubernetes.container.image.pullSecrets", k8sConfig.getImagePullSecret());
        }

        //nfs default val

        params.put("spark.kubernetes.driver.volumes.nfs.cfs.options.server", k8sConfig.getDriverVolumesNfsCfsOptionsServer());
        params.put("spark.kubernetes.driver.volumes.nfs.cfs.options.path", k8sConfig.getDriverVolumesNfsCfsOptionsPath());
        params.put("spark.kubernetes.driver.volumes.nfs.cfs.mount.path", k8sConfig.getDriverVolumesNfsCfsMountPath());
        params.put("spark.kubernetes.driver.volumes.nfs.cfs.mount.readOnly", k8sConfig.getDriverVolumesNfsCfsMountReadOnly());
        params.put("spark.kubernetes.executor.volumes.nfs.cfs.options.server", k8sConfig.getExecutorVolumesNfsCfsOptionsServer());
        params.put("spark.kubernetes.executor.volumes.nfs.cfs.options.path", k8sConfig.getExecutorVolumesNfsCfsOptionsPath());
        params.put("spark.kubernetes.executor.volumes.nfs.cfs.mount.path", k8sConfig.getExecutorVolumesNfsCfsMountPath());
        params.put("spark.kubernetes.executor.volumes.nfs.cfs.mount.readOnly", k8sConfig.getExecutorVolumesNfsCfsMountReadOnly());

        // 2.conf中设置的参数
        if (sparkSubmitParam.getConfs() != null) {
            sparkSubmitParam.getConfs().forEach((k, v) -> {
                params.put(k, v);
            });
        }


        Job job = new Job();
        job.setJobName(name);
        job.setJobFile(sparkSubmitParam.getJarPath());
        job.setJobMainClass(sparkSubmitParam.getMainClass());
        job.setDriverPodName(driverPodName);
        job.setExecutorPodName(executorPodNamePrefix);

        job.setMaster(sparkConfig.getMaster());
        if (null != sparkSubmitParam.getMaster() && sparkSubmitParam.getMaster().startsWith("k8s:")) {
            job.setMaster(sparkSubmitParam.getMaster());
        }


        job.setDeployMode(sparkConfig.getDeployMode());
        job.setJobConf(JSON.toJSONString(params));
        job.setJobArgs(JSON.toJSONString(sparkSubmitParam.getMainArgs()));
        job.setNamespace(k8sConfig.getNamespace());
        job.setJobStatus("CREATED");
        job.setCreateTime(new Date());
        job.setUpdateTime(new Date());
        job.setCallback(sparkSubmitParam.getCallbackEndPoint());
        jobService.save(job);

        JobHistory jobHistory = new JobHistory();
        jobHistory.setJobId(job.getId());
        jobHistory.setJobArgs(JSON.toJSONString(sparkSubmitParam.getMainArgs()));
        jobHistory.setJobConf(JSON.toJSONString(params));
        jobHistory.setJobName(name);
        jobHistory.setDriverPodName(driverPodName);
        jobHistory.setJobStatus(Consts.JOB_STATUS_WAITING);
        jobHistory.setCreateTime(new Date());
        jobHistory.setUpdateTime(new Date());
        jobHistoryService.save(jobHistory);
        return job.getId();
    }

    @GetMapping(value = "/heartbeat")
    public String heartBeat() {
        return "200";
    }
}
