package com.kidslearn.api.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.PracticeMode;
import com.kidslearn.api.mapper.PracticeModeMapper;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminPracticeControllerTest {

    @Test
    void listsPracticeModesByPage() {
        PracticeModeMapper mapper = mock(PracticeModeMapper.class);
        AdminPracticeController controller = new AdminPracticeController(mapper, mock(AdminOperationLogService.class));
        PracticeMode mode = new PracticeMode();
        mode.setId(7L);
        mode.setName("20以内加法");
        Page<PracticeMode> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(mode));
        when(mapper.selectPage(any(), any())).thenReturn(page);

        var result = controller.list(1, 20, 2L);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        assertEquals("20以内加法", result.getData().getList().get(0).getName());
    }

    @Test
    void savesAndDeletesPracticeModesWithAuditLog() {
        PracticeModeMapper mapper = mock(PracticeModeMapper.class);
        AdminOperationLogService logService = mock(AdminOperationLogService.class);
        AdminPracticeController controller = new AdminPracticeController(mapper, logService);
        PracticeMode mode = new PracticeMode();
        mode.setId(8L);

        controller.save(mode);
        controller.delete(8L);

        verify(mapper).updateById(mode);
        verify(mapper).deleteById(8L);
        verify(logService).write("practice", "save", "practice-mode", 8L, "save practice mode");
        verify(logService).write("practice", "delete", "practice-mode", 8L, "delete practice mode");
    }
}
