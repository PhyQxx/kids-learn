package com.kidslearn.api.service.impl;

import com.kidslearn.api.entity.OperationLog;
import com.kidslearn.api.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminOperationLogService {

    private final OperationLogMapper operationLogMapper;

    public void write(String module, String action, String targetType, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }
}
