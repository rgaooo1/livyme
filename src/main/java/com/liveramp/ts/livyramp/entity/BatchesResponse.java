package com.liveramp.ts.livyramp.entity;

import lombok.Data;

import java.util.List;

/**
 * @author robgao
 * @date 2023/8/29 15:23
 * @className BatchesResponse
 */
@Data
public class BatchesResponse {
        private int id;
        private String name;
        private String owner;
        private String proxyUser;
        private String state;
        private String appID;
        private AppInfo appInfo;
        private List<String> log;
}
