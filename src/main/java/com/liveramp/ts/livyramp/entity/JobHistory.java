package com.liveramp.ts.livyramp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * (JobHistory)实体类
 *
 * @author makejava
 * @since 2023-08-29 12:18:43
 */
@Data
@TableName("job_history")
public class JobHistory implements Serializable {
    private static final long serialVersionUID = 315769059653352170L;
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Long jobId;
    private String jobName;
    private String driverPodName;
    private String jobArgs;
    private String jobConf;
    private String jobStatus;
    private String podStatus;
    private String logInfo;
    private String errLog;
    private Date createTime;
    private Date updateTime;
}

