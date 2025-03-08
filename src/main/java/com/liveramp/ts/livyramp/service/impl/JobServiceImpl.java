package com.liveramp.ts.livyramp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liveramp.ts.livyramp.entity.Job;
import com.liveramp.ts.livyramp.mapper.JobMapper;
import com.liveramp.ts.livyramp.service.JobService;
import org.springframework.stereotype.Service;

/**
 * @author robgao
 * @date 2023/9/25 14:34
 * @className JobServiceImpl
 */
@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {
}
