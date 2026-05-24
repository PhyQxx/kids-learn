package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.kidslearn.api.entity.OperationLog;
import com.kidslearn.api.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminOperationLogServiceTest {

    @Test
    void writesOperationLogRows() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        AdminOperationLogService service = new AdminOperationLogService(mapper);

        service.write("content", "delete", "question", 9L, "delete question");

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals("content", captor.getValue().getModule());
        assertEquals("delete", captor.getValue().getAction());
        assertEquals("question", captor.getValue().getTargetType());
        assertEquals(9L, captor.getValue().getTargetId());
        assertEquals("delete question", captor.getValue().getDetail());
    }
}
