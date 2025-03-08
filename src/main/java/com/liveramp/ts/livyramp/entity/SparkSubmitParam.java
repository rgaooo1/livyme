package com.liveramp.ts.livyramp.entity;

import lombok.Data;

import java.util.Map;

@Data
public class SparkSubmitParam {
    String master;
    String deployMode;//可以是Cluster或Client
    String appName;
    String imagePath;
    String jarPath;//应用程序jar包的存放位置，可以是本地或HDFS
    String mainClass;//应用程序的mainClass
    String[] mainArgs;//应用程序的mainClass 传入参数
    Map<String, String> confs;//其它配置，可以有多个，每个以分号隔开
    String runMode;// async or not
    String callbackEndPoint;
    String callbackBody;
}
