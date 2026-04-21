package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.Subject;
import com.kidslearn.api.entity.Course;
import com.kidslearn.api.entity.CourseLevel;
import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.QuestionOption;
import com.kidslearn.api.mapper.*;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "管理后台-内容管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final SubjectMapper subjectMapper;
    private final CourseMapper courseMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;

    // ==================== 学科管理 ====================

    @Operation(summary = "学科列表")
    @GetMapping("/subject/list")
    public R<PageResult<Subject>> subjectList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<Subject>()
            .like(keyword != null && !keyword.isEmpty(), Subject::getSubjectName, keyword)
            .orderByAsc(Subject::getSortOrder);
        Page<Subject> p = subjectMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑学科")
    @PostMapping("/subject/save")
    public R<Void> subjectSave(@RequestBody Subject subject) {
        if (subject.getId() == null) {
            subjectMapper.insert(subject);
        } else {
            subjectMapper.updateById(subject);
        }
        return R.ok();
    }

    @Operation(summary = "删除学科")
    @DeleteMapping("/subject/{id}")
    public R<Void> subjectDelete(@PathVariable Long id) {
        subjectMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 课程管理 ====================

    @Operation(summary = "课程列表")
    @GetMapping("/course/list")
    public R<PageResult<Course>> courseList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
            .eq(subjectId != null, Course::getSubjectId, subjectId)
            .like(keyword != null && !keyword.isEmpty(), Course::getCourseName, keyword)
            .orderByAsc(Course::getSortOrder);
        Page<Course> p = courseMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑课程")
    @PostMapping("/course/save")
    public R<Void> courseSave(@RequestBody Course course) {
        if (course.getId() == null) {
            courseMapper.insert(course);
        } else {
            courseMapper.updateById(course);
        }
        return R.ok();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/course/{id}")
    public R<Void> courseDelete(@PathVariable Long id) {
        courseMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 关卡管理 ====================

    @Operation(summary = "关卡列表")
    @GetMapping("/level/list")
    public R<PageResult<CourseLevel>> levelList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long courseId) {
        LambdaQueryWrapper<CourseLevel> wrapper = new LambdaQueryWrapper<CourseLevel>()
            .eq(courseId != null, CourseLevel::getCourseId, courseId)
            .orderByAsc(CourseLevel::getLevelNum);
        Page<CourseLevel> p = courseLevelMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑关卡")
    @PostMapping("/level/save")
    public R<Void> levelSave(@RequestBody CourseLevel level) {
        if (level.getId() == null) {
            courseLevelMapper.insert(level);
        } else {
            courseLevelMapper.updateById(level);
        }
        return R.ok();
    }

    @Operation(summary = "删除关卡")
    @DeleteMapping("/level/{id}")
    public R<Void> levelDelete(@PathVariable Long id) {
        courseLevelMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 题目管理 ====================

    @Operation(summary = "题目列表")
    @GetMapping("/question/list")
    public R<PageResult<Question>> questionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long courseLevelId,
            @RequestParam(required = false) Integer questionType) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
            .eq(courseLevelId != null, Question::getCourseLevelId, courseLevelId)
            .eq(questionType != null, Question::getQuestionType, questionType)
            .orderByAsc(Question::getSortOrder);
        Page<Question> p = questionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑题目")
    @PostMapping("/question/save")
    public R<Void> questionSave(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        Question question = new Question();
        question.setId(id);
        question.setCourseLevelId(Long.valueOf(body.getOrDefault("courseLevelId", 0).toString()));
        question.setQuestionType(Integer.valueOf(body.getOrDefault("questionType", 1).toString()));
        question.setQuestionContent(body.getOrDefault("questionContent", "").toString());
        question.setDifficulty(Integer.valueOf(body.getOrDefault("difficulty", 1).toString()));
        question.setScore(Integer.valueOf(body.getOrDefault("score", 10).toString()));
        question.setTimeLimit(Integer.valueOf(body.getOrDefault("timeLimit", 0).toString()));
        question.setAnalysis(body.getOrDefault("analysis", "").toString());
        question.setSortOrder(Integer.valueOf(body.getOrDefault("sortOrder", 0).toString()));

        if (id == null) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
            // delete old options
            questionOptionMapper.delete(
                new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id)
            );
        }

        // save options
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) body.get("options");
        if (options != null) {
            for (Map<String, Object> opt : options) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(question.getId());
                option.setOptionLabel(opt.getOrDefault("optionLabel", "").toString());
                option.setOptionContent(opt.getOrDefault("optionContent", "").toString());
                option.setIsCorrect(Integer.valueOf(opt.getOrDefault("isCorrect", 0).toString()));
                option.setSortOrder(Integer.valueOf(opt.getOrDefault("sortOrder", 0).toString()));
                questionOptionMapper.insert(option);
            }
        }
        return R.ok();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/question/{id}")
    public R<Void> questionDelete(@PathVariable Long id) {
        questionMapper.deleteById(id);
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id));
        return R.ok();
    }

    @Operation(summary = "获取题目选项")
    @GetMapping("/question/{id}/options")
    public R<List<QuestionOption>> questionOptions(@PathVariable Long id) {
        List<QuestionOption> options = questionOptionMapper.selectList(
            new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, id).orderByAsc(QuestionOption::getSortOrder)
        );
        return R.ok(options);
    }
}
