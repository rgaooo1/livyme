package com.liveramp.ts.livyramp.threads;

import cn.hutool.core.io.FileUtil;
import com.liveramp.ts.livyramp.config.Consts;
import com.liveramp.ts.livyramp.config.SparkKubernetesConfigProperties;
import com.liveramp.ts.livyramp.entity.JobHistory;
import com.liveramp.ts.livyramp.service.JobHistoryService;
//import com.liveramp.ts.nats.starter.sender.DefaultStreamSender;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;

import java.io.File;
import java.io.IOException;

/**
 * @author robgao
 * @date 2023/8/28 15:48
 * @className SparkJobRun
 */
@Slf4j
public class SparkJobRun {
    SparkLauncher launcher;
    JobHistory jobHistory;
    JobHistoryService jobHistoryService;
    SparkKubernetesConfigProperties k8sConfig;

    public SparkJobRun(SparkLauncher launcher, JobHistory jobHistory, JobHistoryService jobHistoryService, SparkKubernetesConfigProperties k8sConfig) {
        this.launcher = launcher;
        this.jobHistory = jobHistory;
        this.jobHistoryService = jobHistoryService;
        this.k8sConfig = k8sConfig;
    }

    public String run() {
        try {
            File logInfoFile = new File(jobHistory.getDriverPodName() + "-info.log");
            File logErrorFile = new File(jobHistory.getDriverPodName() + "-err.log");
            launcher.redirectOutput(logInfoFile);
            launcher.redirectError(logErrorFile);
            SparkAppHandle handle = launcher.startApplication();
            // 监听应用程序状态
            handle.addListener(new SparkAppHandle.Listener() {
                @Override
                public void stateChanged(SparkAppHandle handle) {
                    System.out.println("Spark application state changed to " + handle.getState());
                    // 检查应用程序是否成功提交
                    if (handle.getState().isFinal()) {
                        if (handle.getState() == SparkAppHandle.State.FINISHED) {
                            System.out.println("Spark application finished successfully!");
                        } else {
                            System.out.println("Spark application failed with state: " + handle.getState());
                        }
                    }
                }

                @Override
                public void infoChanged(SparkAppHandle handle) {
                    // 在这里可以获取有关应用程序的其他信息
                    System.out.println("Spark application info changed: " + handle.getAppId());
                }
            });

            ApiClient apiClient = Config.fromCluster();
            CoreV1Api api = new CoreV1Api(apiClient);

            String state = "";
            while (!state.equalsIgnoreCase("Succeeded") && !state.equalsIgnoreCase("Failed") && !state.equalsIgnoreCase("Unknown")) {
                V1Pod pod = null;
                try {
                    pod = api.readNamespacedPod(jobHistory.getDriverPodName(), k8sConfig.getNamespace(), null);
                    log.info("Pod name: {} , status: {}", jobHistory.getDriverPodName(), pod.getStatus().getPhase());
                    state = pod.getStatus().getPhase();
                    jobHistory.setPodStatus(state);
                    switch (state) {
                        case Consts.POD_STATUS_Pending:
                            jobHistory.setJobStatus(Consts.JOB_STATUS_WAITING);
                            break;
                        case Consts.POD_STATUS_Running:
                            jobHistory.setJobStatus(Consts.JOB_STATUS_RUNNING);
                            String info = FileUtil.readUtf8String(logInfoFile);
                            String error = FileUtil.readUtf8String(logErrorFile);
                            jobHistory.setLogInfo(info);
                            jobHistory.setErrLog(error);
                            break;
                        case Consts.POD_STATUS_Succeeded:
                            jobHistory.setJobStatus(Consts.JOB_STATUS_SUCCESS);
                            V1DeleteOptions deleteOptions = new V1DeleteOptions();
                            // 成功之后删除pod
                            V1Pod v1Pod = api.deleteNamespacedPod(jobHistory.getDriverPodName(), k8sConfig.getNamespace(),null, null, null, null, null, deleteOptions);
                            break;
                        case Consts.POD_STATUS_Failed:
                            jobHistory.setJobStatus(Consts.JOB_STATUS_ERROR);
                            break;
                        case Consts.POD_STATUS_Unknown:
                            jobHistory.setJobStatus(Consts.JOB_STATUS_DISAPPEARED);
                            break;
                    }
                    jobHistoryService.saveOrUpdate(jobHistory);
                } catch (ApiException e) {
                    log.info("pod maybe not created");
                }
                Thread.sleep(5000);
            }

            String info = FileUtil.readUtf8String(logInfoFile);
            String error = FileUtil.readUtf8String(logErrorFile);
            jobHistory.setLogInfo(info);
            jobHistory.setErrLog(error);
            jobHistoryService.saveOrUpdate(jobHistory);
            FileUtil.del(logInfoFile);
            FileUtil.del(logErrorFile);
            return state;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
