package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.PracticeMode;
import com.kidslearn.api.mapper.PracticeModeMapper;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-专项练习")
@RestController
@RequestMapping("/api/v1/admin/practice")
@RequiredArgsConstructor
public class AdminPracticeController {

    private final PracticeModeMapper practiceModeMapper;
    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "专项练习列表")
    @GetMapping("/list")
    public R<PageResult<PracticeMode>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long subjectId) {
        LambdaQueryWrapper<PracticeMode> wrapper = new LambdaQueryWrapper<PracticeMode>()
            .eq(subjectId != null, PracticeMode::getSubjectId, subjectId)
            .orderByAsc(PracticeMode::getSortOrder)
            .orderByAsc(PracticeMode::getId);
        Page<PracticeMode> p = practiceModeMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑专项练习")
    @PostMapping("/save")
    public R<Void> save(@RequestBody PracticeMode mode) {
        if (mode.getId() == null) {
            practiceModeMapper.insert(mode);
        } else {
            practiceModeMapper.updateById(mode);
        }
        adminOperationLogService.write("practice", "save", "practice-mode", mode.getId(), "save practice mode");
        return R.ok();
    }

    @Operation(summary = "删除专项练习")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        practiceModeMapper.deleteById(id);
        adminOperationLogService.write("practice", "delete", "practice-mode", id, "delete practice mode");
        return R.ok();
    }
}
