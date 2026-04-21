package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "家长中心接口")
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
public class ParentController {

    private final UserMapper userMapper;
    private final FamilyMapper familyMapper;
    private final FamilyChildMapper familyChildMapper;
    private final TimeControlMapper timeControlMapper;
    private final DailyStatsMapper dailyStatsMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;

    @Operation(summary = "获取学习报告")
    @GetMapping("/report")
    public R<Map<String, Object>> getReport(
            HttpServletRequest request,
            @RequestParam(required = false) String month) {
        Long userId = (Long) request.getAttribute("userId");

        Map<String, Object> result = new HashMap<>();

        // Parse month or use current month
        LocalDate startOfMonth, endOfMonth;
        if (month != null && !month.isEmpty()) {
            startOfMonth = LocalDate.parse(month + "-01");
            endOfMonth = startOfMonth.plusMonths(1);
        } else {
            startOfMonth = LocalDate.now().withDayOfMonth(1);
            endOfMonth = LocalDate.now().plusMonths(1);
        }

        // Stats overview
        List<DailyStats> monthStats = dailyStatsMapper.selectList(
            new LambdaQueryWrapper<DailyStats>()
                .eq(DailyStats::getUserId, userId)
                .ge(DailyStats::getStatDate, startOfMonth)
                .lt(DailyStats::getStatDate, endOfMonth)
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", monthStats.size());
        stats.put("totalTime", monthStats.stream().mapToInt(DailyStats::getLearnMinutes).sum());
        stats.put("totalQuestions",
            learningRecordMapper.selectCount(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .ge(LearningRecord::getCreateTime, startOfMonth.atStartOfDay())
                    .lt(LearningRecord::getCreateTime, endOfMonth.atStartOfDay())
            )
        );
        long correctCount = learningRecordMapper.selectCount(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getIsPass, 1)
                .ge(LearningRecord::getCreateTime, startOfMonth.atStartOfDay())
                .lt(LearningRecord::getCreateTime, endOfMonth.atStartOfDay())
        );
        long totalCount = (Long) stats.get("totalQuestions");
        stats.put("accuracy", totalCount > 0 ? Math.round(correctCount * 100.0 / totalCount) : 0);
        result.put("stats", stats);

        // Subject distribution
        List<Subject> subjects = subjectMapper.selectList(null);
        List<Map<String, Object>> subjectStats = new ArrayList<>();
        for (Subject s : subjects) {
            Set<Long> courseIds = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getSubjectId, s.getId())
            ).stream().map(Course::getId).collect(Collectors.toSet());
            Set<Long> levelIds = courseLevelMapper.selectList(
                new LambdaQueryWrapper<CourseLevel>().in(!courseIds.isEmpty(), CourseLevel::getCourseId, courseIds)
            ).stream().map(CourseLevel::getId).collect(Collectors.toSet());

            int time = monthStats.stream()
                .filter(ds -> true) // simplified
                .mapToInt(DailyStats::getLearnMinutes).sum();
            // approximate by counting records for this subject's levels
            long subjectRecordCount = !levelIds.isEmpty() ? learningRecordMapper.selectCount(
                new LambdaQueryWrapper<LearningRecord>()
                    .eq(LearningRecord::getUserId, userId)
                    .in(LearningRecord::getCourseLevelId, levelIds)
                    .ge(LearningRecord::getCreateTime, startOfMonth.atStartOfDay())
                    .lt(LearningRecord::getCreateTime, endOfMonth.atStartOfDay())
            ) : 0;
            int subjectTime = (int) subjectRecordCount * 2; // rough estimate: 2 min per record

            Map<String, Object> sm = new HashMap<>();
            sm.put("name", s.getSubjectName());
            sm.put("time", subjectTime);
            sm.put("percent", stats.get("totalTime") instanceof Integer && (int) stats.get("totalTime") > 0
                ? Math.round(subjectTime * 100.0 / (int) stats.get("totalTime")) : 0);
            subjectStats.add(sm);
        }
        result.put("subjectStats", subjectStats);

        // Daily trend
        List<Map<String, Object>> dailyList = monthStats.stream().map(ds -> {
            Map<String, Object> dm = new HashMap<>();
            dm.put("date", ds.getStatDate().toString());
            dm.put("time", ds.getLearnMinutes());
            dm.put("percent", ds.getLearnMinutes() > 0 ? Math.min(100, ds.getLearnMinutes() * 100 / 30) : 0);
            return dm;
        }).collect(Collectors.toList());
        result.put("dailyList", dailyList);

        return R.ok(result);
    }

    @Operation(summary = "获取时间控制设置")
    @GetMapping("/time-control")
    public R<Map<String, Object>> getTimeControl(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        TimeControl tc = timeControlMapper.selectOne(
            new LambdaQueryWrapper<TimeControl>().eq(TimeControl::getChildUserId, userId)
        );

        Map<String, Object> result = new HashMap<>();
        if (tc != null) {
            result.put("dailyLimitMinutes", tc.getDailyLimit());
            result.put("allowedStartTime", tc.getForbiddenStart() != null ? tc.getForbiddenStart().toString() : null);
            result.put("allowedEndTime", tc.getForbiddenEnd() != null ? tc.getForbiddenEnd().toString() : null);
            result.put("restReminder", tc.getIsEnabled() != null && tc.getIsEnabled() == 1);
        } else {
            result.put("dailyLimitMinutes", 60);
            result.put("allowedStartTime", "08:00");
            result.put("allowedEndTime", "21:00");
            result.put("restReminder", true);
        }
        result.put("autoLockAfterTask", false);
        return R.ok(result);
    }

    @Operation(summary = "保存时间控制设置")
    @PutMapping("/time-control")
    public R<Void> saveTimeControl(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");

        TimeControl tc = timeControlMapper.selectOne(
            new LambdaQueryWrapper<TimeControl>().eq(TimeControl::getChildUserId, userId)
        );

        if (tc == null) {
            tc = new TimeControl();
            tc.setChildUserId(userId);
            tc.setIsEnabled(1);
        }

        if (body.containsKey("dailyLimitMinutes")) {
            tc.setDailyLimit((Integer) body.get("dailyLimitMinutes"));
        }
        if (body.containsKey("allowedStartTime")) {
            tc.setForbiddenStart(LocalTime.parse((String) body.get("allowedStartTime")));
        }
        if (body.containsKey("allowedEndTime")) {
            tc.setForbiddenEnd(LocalTime.parse((String) body.get("allowedEndTime")));
        }
        if (body.containsKey("restReminder")) {
            tc.setIsEnabled((Boolean) body.get("restReminder") ? 1 : 0);
        }

        if (tc.getId() != null) {
            timeControlMapper.updateById(tc);
        } else {
            timeControlMapper.insert(tc);
        }

        return R.ok(null);
    }

    @Operation(summary = "获取家庭组信息")
    @GetMapping("/family")
    public R<Map<String, Object>> getFamily(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Map<String, Object> result = new HashMap<>();

        // Find family by parent user id or via family_child
        Family family = familyMapper.selectOne(
            new LambdaQueryWrapper<Family>().eq(Family::getParentUserId, userId)
        );

        if (family == null) {
            // try via family_child
            FamilyChild fc = familyChildMapper.selectOne(
                new LambdaQueryWrapper<FamilyChild>().eq(FamilyChild::getChildUserId, userId)
            );
            if (fc != null) {
                family = familyMapper.selectById(fc.getFamilyId());
            }
        }

        if (family != null) {
            result.put("family", Map.of(
                "familyName", family.getFamilyName() != null ? family.getFamilyName() : "我的家庭",
                "inviteCode", family.getFamilyCode() != null ? family.getFamilyCode() : ""
            ));

            // Get members
            List<Map<String, Object>> members = new ArrayList<>();
            User parent = userMapper.selectById(family.getParentUserId());
            if (parent != null) {
                members.add(Map.of(
                    "id", parent.getId(),
                    "nickname", parent.getNickname() != null ? parent.getNickname() : "家长",
                    "avatar", parent.getAvatar() != null ? parent.getAvatar() : "",
                    "role", "PARENT"
                ));
            }
            List<FamilyChild> children = familyChildMapper.selectList(
                new LambdaQueryWrapper<FamilyChild>().eq(FamilyChild::getFamilyId, family.getId())
            );
            for (FamilyChild fc : children) {
                User child = userMapper.selectById(fc.getChildUserId());
                if (child != null) {
                    members.add(Map.of(
                        "id", child.getId(),
                        "nickname", child.getNickname() != null ? child.getNickname() : "孩子",
                        "avatar", child.getAvatar() != null ? child.getAvatar() : "",
                        "role", "CHILD"
                    ));
                }
            }
            result.put("members", members);
        }

        return R.ok(result);
    }
}
