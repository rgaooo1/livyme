package com.liveramp.ts.livyramp.threads;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liveramp.ts.livyramp.config.Consts;
import com.liveramp.ts.livyramp.config.SparkKubernetesConfigProperties;
import com.liveramp.ts.livyramp.entity.Job;
import com.liveramp.ts.livyramp.entity.JobHistory;
import com.liveramp.ts.livyramp.service.JobHistoryService;
import com.liveramp.ts.livyramp.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


/**
 * @author robgao
 * @date 2023/9/19 13:57
 * @className JobSchedule
 */
@Component
@Slf4j
public class JobSchedule {


    @Value("${pub.msg.subject:liveramp.emr.msg}")
    String subject;

    private final RestTemplate restTemplate;
    private final JobService jobService;
    private final JobHistoryService jobHistoryService;
    private final SparkKubernetesConfigProperties k8sConfig;
//    private final DefaultStreamSender defaultStreamSender;

    public JobSchedule(RestTemplate restTemplate, JobService jobService, JobHistoryService jobHistoryService, SparkKubernetesConfigProperties k8sConfig
            //, DefaultStreamSender defaultStreamSender
    ) {
        this.restTemplate = restTemplate;
        this.jobService = jobService;
        this.jobHistoryService = jobHistoryService;
        this.k8sConfig = k8sConfig;
//        this.defaultStreamSender = defaultStreamSender;
    }


    @Scheduled(fixedRate = 5000)
    public void myScheduledTask() {

        Job job = null;
        boolean exists = jobService.lambdaQuery().eq(Job::getJobStatus, "CREATED").orderByAsc(Job::getCreateTime).count() > 0;
        if (exists) {
            job = jobService.lambdaQuery().eq(Job::getJobStatus, "CREATED").orderByAsc(Job::getCreateTime).last("limit 1").one();
        } else {
            return;
        }

        JobHistory jobHistory = jobHistoryService.getOne(new QueryWrapper<JobHistory>().eq("job_id", job.getId()));

        String jobName = job.getJobName();
        String master = job.getMaster();
        String jobFile = job.getJobFile();
        String jobMainClass = job.getJobMainClass();
        String deployMode = job.getDeployMode();
        String driverPodName = job.getDriverPodName();
        String executorPodName = job.getExecutorPodName();
        String[] args = JSON.parseArray(job.getJobArgs().toString()).toArray(String[]::new);
        String callback = job.getCallback();

        SparkLauncher launcher = new SparkLauncher();
        launcher.setAppName(jobName)
                .setConf("spark.kubernetes.driver.pod.name", driverPodName)
                .setConf("spark.kubernetes.executor.podNamePrefix", executorPodName)
                .setMaster(master)
                .setDeployMode(deployMode)
                .setAppResource(jobFile)
                .setMainClass(jobMainClass)
                .addAppArgs(args);

        JSON.parseObject(job.getJobConf().toString()).forEach((k, v) -> {
            launcher.setConf(k, v.toString());
        });

        job.setJobStatus("PICKED");
        jobService.saveOrUpdate(job);

        SparkJobRun jobRun = new SparkJobRun(launcher, jobHistory, jobHistoryService, k8sConfig);
        String state = jobRun.run();

        if (state.equalsIgnoreCase(Consts.POD_STATUS_Succeeded)) {
            if (null != callback && !callback.trim().equals("")) {
                if (callback.startsWith("http")) {
                    restTemplate.postForLocation(callback, "{}");
                } else if (callback.equalsIgnoreCase("nats")) {
//                    JSONObject rsp = new JSONObject();
//                    rsp.put("jobId", job.getId());
//                    rsp.put("status", state);
//                    rsp.put("job", job);
//                    rsp.put("jobHistory", jobHistory);
//                    try {
//                        defaultStreamSender.send(subject, rsp.toJSONString());
//                    } catch (JetStreamApiException e) {
//                        log.error("send job status to nats JetStreamApiException", e);
//                        throw new RuntimeException(e);
//                    } catch (IOException e) {
//                        log.error("send job status to nats IOException", e);
//                        throw new RuntimeException(e);
//                    }
                }
            }
        }
    }
}
