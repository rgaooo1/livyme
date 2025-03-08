package com.liveramp.ts.livyramp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName job
 */
@Data
@TableName("job")
public class Job implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    private String jobName;

    /**
     * 
     */
    private String jobFile;

    /**
     * 
     */
    private String jobMainClass;

    /**
     * 
     */
    private Object jobConf;

    /**
     * 
     */
    private Object jobArgs;

    /**
     * 
     */
    private String namespace;

    /**
     * 
     */
    private String driverPodName;

    /**
     * 
     */
    private String executorPodName;

    /**
     * 
     */
    private String master;

    /**
     * 
     */
    private String deployMode;

    /**
     * 
     */
    private String jobStatus;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    private String callback;

}