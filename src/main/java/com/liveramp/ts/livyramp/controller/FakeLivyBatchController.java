package com.liveramp.ts.livyramp.controller;

import com.alibaba.fastjson.JSON;
import com.liveramp.ts.livyramp.config.*;
import com.liveramp.ts.livyramp.entity.*;
import com.liveramp.ts.livyramp.mapper.JobHistoryMapper;
import com.liveramp.ts.livyramp.mapper.JobMapper;
import com.liveramp.ts.livyramp.service.JobHistoryService;
import com.liveramp.ts.livyramp.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * @author robgao
 * @date 2023/8/25 12:22
 * @className BatchController
 */
@Slf4j
@RestController
@RequestMapping("/batches")
public class FakeLivyBatchController {

    private final SparkConfigProperties sparkConfig;
    private final SparkS3ConfigProperties s3Config;
    private final SparkKubernetesConfigProperties k8sConfig;
    private final JobService jobService;
    private final JobHistoryService jobHistoryService;

    public FakeLivyBatchController(SparkConfigProperties sparkConfig, SparkS3ConfigProperties s3Config, SparkKubernetesConfigProperties k8sConfig, JobHistoryMapper jobHistoryMapper, JobMapper jobMapper, JobService jobService, JobHistoryService jobHistoryService) {
        this.sparkConfig = sparkConfig;
        this.s3Config = s3Config;
        this.k8sConfig = k8sConfig;

        this.jobService = jobService;
        this.jobHistoryService = jobHistoryService;
    }

    @PostMapping
    public BatchesResponse submitBatches(@RequestBody JobRequest jrq) throws IOException, InterruptedException {

        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String name = jrq.getName().replaceAll("[\u4e00-\u9fa5]+|[\\pP\\pS\\pZ]", "").toLowerCase();

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

        params.put("spark.kubernetes.driver.volumes.nfs.cfs.options.server", k8sConfig.getDriverVolumesNfsCfsOptionsServer());
        params.put("spark.kubernetes.driver.volumes.nfs.cfs.options.path", k8sConfig.getDriverVolumesNfsCfsOptionsPath());
        params.put("spark.kubernetes.driver.volumes.nfs.cfs.mount.path", k8sConfig.getDriverVolumesNfsCfsMountPath());
        params.put("spark.kubernetes.driver.volumes.nfs.cfs.mount.readOnly", k8sConfig.getDriverVolumesNfsCfsMountReadOnly());
        params.put("spark.kubernetes.executor.volumes.nfs.cfs.options.server", k8sConfig.getExecutorVolumesNfsCfsOptionsServer());
        params.put("spark.kubernetes.executor.volumes.nfs.cfs.options.path", k8sConfig.getExecutorVolumesNfsCfsOptionsPath());
        params.put("spark.kubernetes.executor.volumes.nfs.cfs.mount.path", k8sConfig.getExecutorVolumesNfsCfsMountPath());
        params.put("spark.kubernetes.executor.volumes.nfs.cfs.mount.readOnly", k8sConfig.getExecutorVolumesNfsCfsMountReadOnly());

        // 2. requestBody中的参数
        if (jrq.getDriverCores() != null && jrq.getDriverCores() > 0) {
            params.put("spark.driver.cores", String.valueOf(jrq.getDriverCores()));
        }
        if (Strings.isNotBlank(jrq.getDriverMemory())) {
            params.put(SparkLauncher.DRIVER_MEMORY, jrq.getDriverMemory());
        }
        if (jrq.getExecutorCores() != null && jrq.getExecutorCores() > 0) {
            params.put(SparkLauncher.EXECUTOR_CORES, String.valueOf(jrq.getExecutorCores()));
        }
        if (Strings.isNotBlank(jrq.getExecutorMemory())) {
            params.put(SparkLauncher.EXECUTOR_MEMORY, jrq.getExecutorMemory());
        }
        if (jrq.getNumExecutors() != null && jrq.getNumExecutors().intValue() > 0) {
            params.put("spark.executor.instances", String.valueOf(jrq.getNumExecutors()));
        }

        // 3.conf中设置的参数
        if (jrq.getConf() != null) {
            jrq.getConf().forEach((k, v) -> {
                params.put(k, v);
            });
        }


        // JOB 对象入库, JobPicker 会从入库的Job记录中读取JOB并执行
        Job job = new Job();
        job.setJobName(name);
        job.setJobFile(jrq.getFile());
        job.setJobMainClass(jrq.getClassName());
        job.setDriverPodName(driverPodName);
        job.setExecutorPodName(executorPodNamePrefix);
        job.setMaster(sparkConfig.getMaster());

        if (null != jrq.getMaster() && jrq.getMaster().startsWith("k8s:")) {
            job.setMaster(jrq.getMaster());
        }

        job.setDeployMode(jrq.getDeployMode());
        job.setJobConf(JSON.toJSONString(params));
        job.setJobArgs(JSON.toJSONString(jrq.getArgs()));
        job.setNamespace(k8sConfig.getNamespace());
        job.setJobStatus("CREATED");
        job.setCreateTime(new Date());
        job.setUpdateTime(new Date());
        jobService.save(job);


        // JOB HISTORY 对象入库
        JobHistory jobHistory = new JobHistory();
        jobHistory.setJobId(job.getId());
        jobHistory.setJobArgs(JSON.toJSONString(jrq.getArgs()));
        jobHistory.setJobConf(JSON.toJSONString(params));
        jobHistory.setJobName(jrq.getName());
        jobHistory.setDriverPodName(driverPodName);
        jobHistory.setJobStatus(Consts.JOB_STATUS_WAITING);
        jobHistory.setCreateTime(new Date());
        jobHistory.setUpdateTime(new Date());
        jobHistoryService.save(jobHistory);


        BatchesResponse response = new BatchesResponse();
        response.setId(jobHistory.getId());
        response.setAppID(jobHistory.getDriverPodName());
        response.setState(jobHistory.getJobStatus());
        response.setLog(new ArrayList<>());
        response.setAppInfo(new AppInfo());
        response.setName(jobHistory.getDriverPodName());
        return response;
    }

    @GetMapping("{id}")
    public BatchesResponse getBatchesStatus(@PathVariable("id") int id) {
        JobHistory jobHistory = jobHistoryService.getById(id);
        BatchesResponse response = new BatchesResponse();
        if (jobHistory == null) {
            response.setState(Consts.JOB_STATUS_DISAPPEARED);
        } else {
            response.setId(jobHistory.getId());
            response.setState(jobHistory.getJobStatus());
            List<String> logs = new ArrayList<>();
            logs.add(jobHistory.getLogInfo());
            logs.add(jobHistory.getErrLog());
            response.setLog(logs);
            response.setAppInfo(new AppInfo());
            response.setName(jobHistory.getDriverPodName());
            response.setAppID(jobHistory.getDriverPodName());
        }
        return response;
    }
}
