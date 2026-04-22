package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.CourseGrade;
import com.kidslearn.api.entity.GradeLevel;
import com.kidslearn.api.mapper.CourseGradeMapper;
import com.kidslearn.api.mapper.GradeLevelMapper;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理后台-年级管理")
@RestController
@RequestMapping("/api/v1/admin/grade-level")
@RequiredArgsConstructor
public class AdminGradeLevelController {

    private final GradeLevelMapper gradeLevelMapper;
    private final CourseGradeMapper courseGradeMapper;

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
        // remove course associations
        courseGradeMapper.delete(
            new LambdaQueryWrapper<CourseGrade>().eq(CourseGrade::getGradeLevelId, id)
        );
        gradeLevelMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "设置课程的年级关联")
    @PostMapping("/course-bind")
    public R<Void> bindCourseGrades(@RequestBody Map<String, Object> body) {
        Long courseId = Long.valueOf(body.get("courseId").toString());
        @SuppressWarnings("unchecked")
        List<Number> gradeIds = (List<Number>) body.get("gradeLevelIds");

        // delete old associations
        courseGradeMapper.delete(
            new LambdaQueryWrapper<CourseGrade>().eq(CourseGrade::getCourseId, courseId)
        );
        // insert new
        if (gradeIds != null) {
            for (Number gid : gradeIds) {
                CourseGrade cg = new CourseGrade();
                cg.setCourseId(courseId);
                cg.setGradeLevelId(gid.longValue());
                courseGradeMapper.insert(cg);
            }
        }
        return R.ok();
    }

    @Operation(summary = "获取课程关联的年级ID列表")
    @GetMapping("/course-grades")
    public R<List<Long>> getCourseGrades(@RequestParam Long courseId) {
        List<Long> gradeIds = courseGradeMapper.selectList(
            new LambdaQueryWrapper<CourseGrade>().eq(CourseGrade::getCourseId, courseId)
        ).stream().map(CourseGrade::getGradeLevelId).toList();
        return R.ok(gradeIds);
    }
}
