package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.realtime.RealtimeSessionRegistry;
import com.kidslearn.api.service.AiService;
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
    private final SubjectMapper subjectMapper;
    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final AiService aiService;

    @Operation(summary = "获取学习报告")
    @GetMapping("/report")
    public R<Map<String, Object>> getReport(
            HttpServletRequest request,
            @RequestParam(required = false) String month) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(buildReport(userId, month));
    }

    @Operation(summary = "生成家长AI学习总结")
    @GetMapping("/ai-summary")
    public R<Map<String, Object>> getAiSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> report = buildReport(userId, null);
        return R.ok(normalizeAiSummary(aiService.generateParentSummary(report, Map.of())));
    }

    private Map<String, Object> buildReport(Long userId, String month) {
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

        // All month learning records for detailed stats
        List<LearningRecord> allMonthRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getCreateTime, startOfMonth.atStartOfDay())
                .lt(LearningRecord::getCreateTime, endOfMonth.atStartOfDay())
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", monthStats.size());
        stats.put("totalTime", monthStats.stream().mapToInt(DailyStats::getLearnMinutes).sum());
        stats.put("completedLevels", monthStats.stream()
            .mapToInt(ds -> ds.getCompletedLevels() != null ? ds.getCompletedLevels() : 0).sum());
        stats.put("totalQuestions", (long) allMonthRecords.size());
        long correctCount = allMonthRecords.stream()
            .filter(r -> Integer.valueOf(1).equals(r.getIsPass())).count();
        stats.put("accuracy", allMonthRecords.size() > 0
            ? Math.round(correctCount * 100.0 / allMonthRecords.size()) : 0);
        result.put("stats", stats);

        // Subject distribution with real time
        List<Subject> subjects = subjectMapper.selectList(null);
        List<Map<String, Object>> subjectStats = new ArrayList<>();
        int totalSubjectTime = 0;
        for (Subject s : subjects) {
            Set<Long> levelIds = courseLevelMapper.selectList(
                new LambdaQueryWrapper<CourseLevel>().eq(CourseLevel::getSubjectId, s.getId())
            ).stream().map(CourseLevel::getId).collect(Collectors.toSet());
            if (levelIds.isEmpty()) continue;
            int subjectTime = allMonthRecords.stream()
                .filter(r -> levelIds.contains(r.getCourseLevelId()))
                .mapToInt(r -> r.getAnswerTime() != null ? Math.max(1, r.getAnswerTime() / 60) : 1)
                .sum();
            totalSubjectTime += subjectTime;
            Map<String, Object> sm = new HashMap<>();
            sm.put("name", s.getSubjectName());
            sm.put("time", subjectTime);
            sm.put("percent", 0);
            subjectStats.add(sm);
        }
        for (Map<String, Object> sm : subjectStats) {
            int t = (int) sm.get("time");
            sm.put("percent", totalSubjectTime > 0 ? Math.round(t * 100.0 / totalSubjectTime) : 0);
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

        // === Today Stats ===
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        DailyStats todayDailyStats = dailyStatsMapper.selectOne(
            new LambdaQueryWrapper<DailyStats>()
                .eq(DailyStats::getUserId, userId)
                .eq(DailyStats::getStatDate, today)
        );
        List<LearningRecord> todayRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getPlayTime, todayStart)
                .orderByAsc(LearningRecord::getPlayTime)
        );
        int todayTotalQ = 0;
        int todayCorrectQ = 0;
        for (LearningRecord r : todayRecords) {
            CourseLevel lv = courseLevelMapper.selectById(r.getCourseLevelId());
            int q = lv != null && lv.getTotalQuestions() != null ? lv.getTotalQuestions() : 0;
            todayTotalQ += q;
            todayCorrectQ += Math.max(0, q - (r.getWrongCount() != null ? r.getWrongCount() : 0));
        }
        Map<String, Object> todayMap = new HashMap<>();
        todayMap.put("learnMinutes", todayDailyStats != null && todayDailyStats.getLearnMinutes() != null
            ? todayDailyStats.getLearnMinutes() : 0);
        todayMap.put("completedLevels", todayDailyStats != null && todayDailyStats.getCompletedLevels() != null
            ? todayDailyStats.getCompletedLevels() : 0);
        todayMap.put("accuracy", todayTotalQ > 0 ? Math.round(todayCorrectQ * 100.0 / todayTotalQ) : 0);
        result.put("today", todayMap);

        // === Today Records ===
        List<Map<String, Object>> todayRecordList = new ArrayList<>();
        for (LearningRecord r : todayRecords) {
            CourseLevel lv = courseLevelMapper.selectById(r.getCourseLevelId());
            Subject subj = lv != null && lv.getSubjectId() != null ? subjectMapper.selectById(lv.getSubjectId()) : null;
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", r.getId());
            rec.put("subjectName", subj != null ? subj.getSubjectName() : "未知");
            rec.put("levelName", lv != null ? lv.getLevelName() : "");
            rec.put("playTime", r.getPlayTime() != null
                ? r.getPlayTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
            rec.put("durationMinutes", r.getAnswerTime() != null ? Math.max(1, r.getAnswerTime() / 60) : 1);
            rec.put("isPass", Integer.valueOf(1).equals(r.getIsPass()));
            rec.put("score", r.getScore() != null ? r.getScore() : 0);
            todayRecordList.add(rec);
        }
        result.put("todayRecords", todayRecordList);

        return result;
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

    @Operation(summary = "获取家长实时监控")
    @GetMapping("/realtime-monitor")
    public R<Map<String, Object>> getRealtimeMonitor(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(buildRealtimeMonitor(userId));
    }

    private Map<String, Object> buildRealtimeMonitor(Long userId) {
        Family family = findFamily(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (family == null) {
            result.put("family", Map.of("familyName", "", "inviteCode", ""));
            result.put("children", List.of());
            result.put("summary", buildMonitorSummary(List.of()));
            return result;
        }

        result.put("family", Map.of(
            "familyName", family.getFamilyName() != null ? family.getFamilyName() : "我的家庭",
            "inviteCode", family.getFamilyCode() != null ? family.getFamilyCode() : ""
        ));

        Set<Long> onlineUserIds = realtimeSessionRegistry.connectedUserIds();
        List<FamilyChild> children = familyChildMapper.selectList(
            new LambdaQueryWrapper<FamilyChild>().eq(FamilyChild::getFamilyId, family.getId())
        );
        List<Map<String, Object>> childSnapshots = new ArrayList<>();
        for (FamilyChild fc : children) {
            Map<String, Object> childSnapshot = buildChildMonitorSnapshot(fc, onlineUserIds.contains(fc.getChildUserId()));
            if (!childSnapshot.isEmpty()) {
                childSnapshots.add(childSnapshot);
            }
        }

        result.put("children", childSnapshots);
        result.put("summary", buildMonitorSummary(childSnapshots));
        return result;
    }

    private Map<String, Object> normalizeAiSummary(Map<String, Object> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", raw != null ? Objects.toString(raw.getOrDefault("summary", ""), "") : "");
        result.put("highlights", normalizeStringList(raw != null ? raw.get("highlights") : null));
        result.put("concerns", normalizeStringList(raw != null ? raw.get("concerns") : null));
        result.put("suggestions", normalizeStringList(raw != null ? raw.get("suggestions") : null));
        return result;
    }

    private List<String> normalizeStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .filter(text -> !text.isBlank())
            .collect(Collectors.toList());
    }

    private Family findFamily(Long userId) {
        Family family = familyMapper.selectOne(
            new LambdaQueryWrapper<Family>().eq(Family::getParentUserId, userId).last("LIMIT 1")
        );
        if (family != null) {
            return family;
        }
        FamilyChild fc = familyChildMapper.selectOne(
            new LambdaQueryWrapper<FamilyChild>().eq(FamilyChild::getChildUserId, userId).last("LIMIT 1")
        );
        return fc != null ? familyMapper.selectById(fc.getFamilyId()) : null;
    }

    private Map<String, Object> buildChildMonitorSnapshot(FamilyChild familyChild, boolean online) {
        User child = userMapper.selectById(familyChild.getChildUserId());
        if (child == null) {
            return Map.of();
        }

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        List<LearningRecord> todayRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, child.getId())
                .ge(LearningRecord::getPlayTime, todayStart)
        );
        LearningRecord latestRecord = learningRecordMapper.selectOne(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, child.getId())
                .orderByDesc(LearningRecord::getPlayTime)
                .last("LIMIT 1")
        );

        CourseLevel latestLevel = latestRecord != null ? courseLevelMapper.selectById(latestRecord.getCourseLevelId()) : null;
        Subject latestSubject = latestLevel != null && latestLevel.getSubjectId() != null ? subjectMapper.selectById(latestLevel.getSubjectId()) : null;
        TimeControl timeControl = timeControlMapper.selectOne(
            new LambdaQueryWrapper<TimeControl>()
                .eq(TimeControl::getChildUserId, child.getId())
                .last("LIMIT 1")
        );
        int totalQuestions = sumTotalQuestions(todayRecords);
        int correctCount = sumCorrectQuestions(todayRecords);

        Map<String, Object> map = new HashMap<>();
        map.put("childId", child.getId());
        map.put("nickname", child.getNickname() != null ? child.getNickname() : "孩子");
        map.put("avatar", child.getAvatar() != null ? child.getAvatar() : "");
        map.put("online", online);
        map.put("status", monitorStatus(online, latestRecord, timeControl, todayStart));
        map.put("lastActivityAt", latestRecord != null && latestRecord.getPlayTime() != null
            ? latestRecord.getPlayTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
        map.put("todayMinutes", sumLearningMinutes(todayRecords));
        map.put("dailyLimitMinutes", timeControl != null && timeControl.getDailyLimit() != null ? timeControl.getDailyLimit() : 60);
        map.put("completedLevels", (int) todayRecords.stream().filter(r -> Integer.valueOf(1).equals(r.getIsPass())).count());
        map.put("totalQuestions", totalQuestions);
        map.put("correctCount", correctCount);
        map.put("accuracy", totalQuestions > 0 ? Math.round(correctCount * 100.0 / totalQuestions) : 0);
        map.put("currentSubjectName", latestSubject != null ? latestSubject.getSubjectName() : "");
        map.put("currentLevelName", latestLevel != null ? latestLevel.getLevelName() : "");
        map.put("latestScore", latestRecord != null && latestRecord.getScore() != null ? latestRecord.getScore() : 0);
        map.put("stars", latestRecord != null && latestRecord.getStars() != null ? latestRecord.getStars() : 0);
        map.put("isPass", latestRecord != null && Integer.valueOf(1).equals(latestRecord.getIsPass()));
        return map;
    }

    private String monitorStatus(boolean online, LearningRecord latestRecord, TimeControl timeControl, LocalDateTime todayStart) {
        if (!online) {
            return "OFFLINE";
        }
        if (timeControl != null && Integer.valueOf(1).equals(timeControl.getIsEnabled()) && timeControl.getDailyLimit() != null) {
            DailyStats stats = dailyStatsMapper.selectOne(
                new LambdaQueryWrapper<DailyStats>()
                    .eq(DailyStats::getUserId, timeControl.getChildUserId())
                    .eq(DailyStats::getStatDate, LocalDate.now())
                    .last("LIMIT 1")
            );
            if (stats != null && stats.getLearnMinutes() != null && stats.getLearnMinutes() >= timeControl.getDailyLimit()) {
                return "LIMITED";
            }
        }
        if (latestRecord != null && latestRecord.getPlayTime() != null && latestRecord.getPlayTime().isAfter(todayStart)) {
            return "LEARNING";
        }
        return "ONLINE";
    }

    private int sumLearningMinutes(List<LearningRecord> records) {
        return records.stream()
            .mapToInt(record -> Math.max(1, (record.getAnswerTime() == null ? 0 : record.getAnswerTime()) / 60))
            .sum();
    }

    private int sumTotalQuestions(List<LearningRecord> records) {
        int total = 0;
        for (LearningRecord record : records) {
            CourseLevel level = courseLevelMapper.selectById(record.getCourseLevelId());
            total += level != null && level.getTotalQuestions() != null ? level.getTotalQuestions() : 0;
        }
        return total;
    }

    private int sumCorrectQuestions(List<LearningRecord> records) {
        int correct = 0;
        for (LearningRecord record : records) {
            CourseLevel level = courseLevelMapper.selectById(record.getCourseLevelId());
            int total = level != null && level.getTotalQuestions() != null ? level.getTotalQuestions() : 0;
            correct += Math.max(0, total - (record.getWrongCount() == null ? 0 : record.getWrongCount()));
        }
        return correct;
    }

    private Map<String, Object> buildMonitorSummary(List<Map<String, Object>> children) {
        int onlineCount = 0;
        int learningCount = 0;
        int todayMinutes = 0;
        int completedLevels = 0;
        int alertCount = 0;
        for (Map<String, Object> child : children) {
            if (Boolean.TRUE.equals(child.get("online"))) {
                onlineCount++;
            }
            if ("LEARNING".equals(child.get("status"))) {
                learningCount++;
            }
            todayMinutes += ((Number) child.getOrDefault("todayMinutes", 0)).intValue();
            completedLevels += ((Number) child.getOrDefault("completedLevels", 0)).intValue();
            int limit = ((Number) child.getOrDefault("dailyLimitMinutes", 0)).intValue();
            if (limit > 0 && ((Number) child.getOrDefault("todayMinutes", 0)).intValue() >= limit) {
                alertCount++;
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("childCount", children.size());
        summary.put("onlineCount", onlineCount);
        summary.put("learningCount", learningCount);
        summary.put("todayMinutes", todayMinutes);
        summary.put("completedLevels", completedLevels);
        summary.put("alertCount", alertCount);
        return summary;
    }
}
