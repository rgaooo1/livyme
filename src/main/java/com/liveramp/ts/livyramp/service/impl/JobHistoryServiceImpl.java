package com.liveramp.ts.livyramp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liveramp.ts.livyramp.entity.JobHistory;
import com.liveramp.ts.livyramp.mapper.JobHistoryMapper;
import com.liveramp.ts.livyramp.service.JobHistoryService;
import org.springframework.stereotype.Service;

/**
 * @author robgao
 * @date 2023/9/25 14:36
 * @className JobHistoryServiceImpl
 */
@Service
public class JobHistoryServiceImpl  extends ServiceImpl<JobHistoryMapper, JobHistory> implements JobHistoryService {
}
