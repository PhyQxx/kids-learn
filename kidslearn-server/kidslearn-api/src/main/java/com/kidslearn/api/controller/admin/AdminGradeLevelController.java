package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.GradeLevel;
import com.kidslearn.api.mapper.GradeLevelMapper;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理后台-年级管理")
@RestController
@RequestMapping("/api/v1/admin/grade-level")
@RequiredArgsConstructor
public class AdminGradeLevelController {

    private final GradeLevelMapper gradeLevelMapper;
    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "年级列表")
    @GetMapping("/list")
    public R<List<GradeLevel>> list(@RequestParam(required = false) Integer ageGroup) {
        LambdaQueryWrapper<GradeLevel> wrapper = new LambdaQueryWrapper<GradeLevel>()
            .eq(ageGroup != null, GradeLevel::getAgeGroup, ageGroup)
            .orderByAsc(GradeLevel::getLevelCode);
        return R.ok(gradeLevelMapper.selectList(wrapper));
    }

    @Operation(summary = "新增/编辑年级")
    @PostMapping("/save")
    public R<Void> save(@RequestBody GradeLevel gradeLevel) {
        if (gradeLevel.getId() == null) {
            gradeLevelMapper.insert(gradeLevel);
        } else {
            gradeLevelMapper.updateById(gradeLevel);
        }
        return R.ok();
    }

    @Operation(summary = "删除年级")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        gradeLevelMapper.deleteById(id);
        adminOperationLogService.write("grade-level", "delete", "grade-level", id, "delete grade level");
        return R.ok();
    }
}
