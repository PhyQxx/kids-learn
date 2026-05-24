package com.kidslearn.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kidslearn.api.entity.AppVersion;
import com.kidslearn.api.entity.ContentAudit;
import com.kidslearn.api.entity.CourseLevel;
import com.kidslearn.api.entity.DailyStats;
import com.kidslearn.api.entity.LearningRecord;
import com.kidslearn.api.entity.OperationLog;
import com.kidslearn.api.entity.Order;
import com.kidslearn.api.entity.Question;
import com.kidslearn.api.entity.Subject;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.AppVersionMapper;
import com.kidslearn.api.mapper.ContentAuditMapper;
import com.kidslearn.api.mapper.CourseLevelMapper;
import com.kidslearn.api.mapper.DailyStatsMapper;
import com.kidslearn.api.mapper.LearningRecordMapper;
import com.kidslearn.api.mapper.OperationLogMapper;
import com.kidslearn.api.mapper.OrderMapper;
import com.kidslearn.api.mapper.QuestionMapper;
import com.kidslearn.api.mapper.SubjectMapper;
import com.kidslearn.api.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardStatsService {

    private static final DateTimeFormatter ACTIVITY_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] WEEK_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final String[] SUBJECT_COLORS = {"#FF6B6B", "#4ECDC4", "#F6C85F", "#6C8CFF", "#A78BFA", "#38BDF8"};

    private final UserMapper userMapper;
    private final CourseLevelMapper courseLevelMapper;
    private final OrderMapper orderMapper;
    private final QuestionMapper questionMapper;
    private final SubjectMapper subjectMapper;
    private final DailyStatsMapper dailyStatsMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final ContentAuditMapper contentAuditMapper;
    private final AppVersionMapper appVersionMapper;
    private final OperationLogMapper operationLogMapper;

    public Map<String, Object> getStats() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().ne(User::getUserType, 3));
        long todayActiveUsers = dailyStatsMapper.selectCount(new LambdaQueryWrapper<DailyStats>().eq(DailyStats::getStatDate, today));
        long totalLevels = courseLevelMapper.selectCount(new LambdaQueryWrapper<>());
        long enabledLevels = courseLevelMapper.selectCount(new LambdaQueryWrapper<CourseLevel>().eq(CourseLevel::getStatus, 1));
        long totalQuestions = questionMapper.selectCount(new LambdaQueryWrapper<>());
        long totalOrders = orderMapper.selectCount(new LambdaQueryWrapper<>());
        long passedRecords = learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
            .ge(LearningRecord::getPlayTime, startTime)
            .lt(LearningRecord::getPlayTime, tomorrowStart)
            .eq(LearningRecord::getIsPass, 1));
        long totalRecentRecords = learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
            .ge(LearningRecord::getPlayTime, startTime)
            .lt(LearningRecord::getPlayTime, tomorrowStart));
        long missingAudioQuestions = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
            .and(wrapper -> wrapper.isNull(Question::getQuestionContent)
                .or()
                .notLike(Question::getQuestionContent, "\"audioUrl\"")));
        long pendingAudits = contentAuditMapper.selectCount(new LambdaQueryWrapper<ContentAudit>().eq(ContentAudit::getStatus, 0));
        long pendingItems = missingAudioQuestions + pendingAudits;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("normalUsers", totalUsers);
        stats.put("todayActiveUsers", todayActiveUsers);
        stats.put("totalLevels", totalLevels);
        stats.put("totalQuestions", totalQuestions);
        stats.put("totalOrders", totalOrders);
        stats.put("completionRate", percent(passedRecords, totalRecentRecords));
        stats.put("pendingItems", pendingItems);
        stats.put("todayRevenue", todayRevenue(today, tomorrowStart));
        stats.put("trendData", trendData(start, today, startTime, tomorrowStart));
        stats.put("funnelData", funnelData(totalUsers, totalRecentRecords, passedRecords));
        stats.put("subjectPerformance", subjectPerformance(startTime, tomorrowStart));
        stats.put("contentHealth", contentHealth(totalQuestions, missingAudioQuestions, totalLevels, enabledLevels));
        stats.put("todos", todos(missingAudioQuestions, pendingAudits));
        stats.put("recentActivities", recentActivities());
        return stats;
    }

    static int percent(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return (int) Math.round(numerator * 100.0 / denominator);
    }

    private long todayRevenue(LocalDate today, LocalDateTime tomorrowStart) {
        List<Map<String, Object>> rows = orderMapper.selectMaps(new QueryWrapper<Order>()
            .select("COALESCE(SUM(amount), 0) AS revenue")
            .eq("pay_status", 1)
            .ge("pay_time", today.atStartOfDay())
            .lt("pay_time", tomorrowStart));
        if (rows.isEmpty() || rows.get(0).get("revenue") == null) return 0L;
        Number revenue = (Number) rows.get(0).get("revenue");
        return Math.round(revenue.doubleValue());
    }

    private List<Map<String, Object>> trendData(LocalDate start, LocalDate today, LocalDateTime startTime, LocalDateTime tomorrowStart) {
        Map<LocalDate, DailyLearningRow> learningByDate = learningRecordMapper.selectMaps(new QueryWrapper<LearningRecord>()
                .select("DATE(play_time) AS statDate", "COUNT(*) AS answers", "COALESCE(SUM(CASE WHEN is_pass = 1 THEN 1 ELSE 0 END), 0) AS passed")
                .ge("play_time", startTime)
                .lt("play_time", tomorrowStart)
                .groupBy("DATE(play_time)"))
            .stream()
            .map(row -> new DailyLearningRow(toLocalDate(row.get("statDate")), toLong(row.get("answers")), toLong(row.get("passed"))))
            .filter(row -> row.statDate() != null)
            .collect(Collectors.toMap(DailyLearningRow::statDate, row -> row, (left, right) -> left));

        Map<LocalDate, Long> activeByDate = dailyStatsMapper.selectMaps(new QueryWrapper<DailyStats>()
                .select("stat_date AS statDate", "COUNT(DISTINCT user_id) AS activeUsers")
                .between("stat_date", start, today)
                .groupBy("stat_date"))
            .stream()
            .collect(Collectors.toMap(
                row -> toLocalDate(row.get("statDate")),
                row -> toLong(row.get("activeUsers")),
                (left, right) -> left
            ));

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            DailyLearningRow learning = learningByDate.getOrDefault(day, new DailyLearningRow(day, 0L, 0L));
            long activeUsers = activeByDate.getOrDefault(day, 0L);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", WEEK_LABELS[day.getDayOfWeek().getValue() - 1]);
            item.put("answers", learning.answers());
            item.put("active", activeUsers);
            item.put("finish", percent(learning.passed(), learning.answers()));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> funnelData(long totalUsers, long totalRecentRecords, long passedRecords) {
        long firstChallengeUsers = learningRecordMapper.selectMaps(new QueryWrapper<LearningRecord>()
                .select("COUNT(DISTINCT user_id) AS count"))
            .stream()
            .findFirst()
            .map(row -> toLong(row.get("count")))
            .orElse(0L);
        long reviewUsers = learningRecordMapper.selectMaps(new QueryWrapper<LearningRecord>()
                .select("COUNT(DISTINCT user_id) AS count")
                .gt("wrong_count", 0))
            .stream()
            .findFirst()
            .map(row -> toLong(row.get("count")))
            .orElse(0L);

        return List.of(
            funnelItem("注册用户", 100, totalUsers, "#FF6B6B"),
            funnelItem("首次闯关", percent(firstChallengeUsers, totalUsers), firstChallengeUsers, "#4ECDC4"),
            funnelItem("完成一关", percent(passedRecords, totalRecentRecords), passedRecords, "#F6C85F"),
            funnelItem("产生错题", percent(reviewUsers, totalUsers), reviewUsers, "#6C8CFF")
        );
    }

    private Map<String, Object> funnelItem(String label, int rate, long count, String color) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("rate", rate);
        item.put("count", count);
        item.put("color", color);
        return item;
    }

    private List<Map<String, Object>> subjectPerformance(LocalDateTime startTime, LocalDateTime tomorrowStart) {
        Map<Long, Long> recordCountByLevel = learningRecordMapper.selectMaps(new QueryWrapper<LearningRecord>()
                .select("course_level_id AS levelId", "COUNT(*) AS count")
                .ge("play_time", startTime)
                .lt("play_time", tomorrowStart)
                .isNotNull("course_level_id")
                .groupBy("course_level_id"))
            .stream()
            .collect(Collectors.toMap(row -> toLong(row.get("levelId")), row -> toLong(row.get("count")), Long::sum));

        Map<Long, Long> subjectCounts = new HashMap<>();
        if (!recordCountByLevel.isEmpty()) {
            courseLevelMapper.selectList(new LambdaQueryWrapper<CourseLevel>().in(CourseLevel::getId, recordCountByLevel.keySet()))
                .forEach(level -> subjectCounts.merge(level.getSubjectId(), recordCountByLevel.getOrDefault(level.getId(), 0L), Long::sum));
        }

        long total = subjectCounts.values().stream().mapToLong(Long::longValue).sum();
        Map<Long, Subject> subjects = subjectMapper.selectList(new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getSortOrder))
            .stream()
            .collect(Collectors.toMap(Subject::getId, subject -> subject, (left, right) -> left, LinkedHashMap::new));

        List<Map<String, Object>> result = new ArrayList<>();
        int colorIndex = 0;
        for (Map.Entry<Long, Subject> entry : subjects.entrySet()) {
            long count = subjectCounts.getOrDefault(entry.getKey(), 0L);
            if (count == 0 && total > 0) continue;
            Subject subject = entry.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", subject.getSubjectName());
            item.put("value", percent(count, total));
            item.put("count", count);
            item.put("color", subject.getColor() != null && !subject.getColor().isBlank()
                ? subject.getColor()
                : SUBJECT_COLORS[colorIndex % SUBJECT_COLORS.length]);
            result.add(item);
            colorIndex++;
        }
        return result.stream().limit(6).toList();
    }

    private List<Map<String, Object>> contentHealth(long totalQuestions, long missingAudioQuestions, long totalLevels, long enabledLevels) {
        long activeSubjects = subjectMapper.selectCount(new LambdaQueryWrapper<Subject>().eq(Subject::getStatus, 1));
        long subjectsWithQuestions = questionMapper.selectMaps(new QueryWrapper<Question>()
                .select("COUNT(DISTINCT subject_id) AS count")
                .isNotNull("subject_id"))
            .stream()
            .findFirst()
            .map(row -> toLong(row.get("count")))
            .orElse(0L);

        return List.of(
            healthItem("题目覆盖", "有题目的启用学科占比", percent(subjectsWithQuestions, activeSubjects), "#4ECDC4"),
            healthItem("音频覆盖", "题目朗读音频覆盖", percent(totalQuestions - missingAudioQuestions, totalQuestions), "#FF6B6B"),
            healthItem("关卡启用", "已上线关卡占比", percent(enabledLevels, totalLevels), "#F6C85F")
        );
    }

    private Map<String, Object> healthItem(String label, String desc, int value, String color) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("desc", desc);
        item.put("value", value);
        item.put("color", color);
        return item;
    }

    private List<Map<String, Object>> todos(long missingAudioQuestions, long pendingAudits) {
        long disabledLevels = courseLevelMapper.selectCount(new LambdaQueryWrapper<CourseLevel>().ne(CourseLevel::getStatus, 1));
        long versions = appVersionMapper.selectCount(new LambdaQueryWrapper<AppVersion>());
        return List.of(
            todoItem("题库音频待生成", missingAudioQuestions, "danger"),
            todoItem("内容审核待处理", pendingAudits, pendingAudits > 0 ? "danger" : "normal"),
            todoItem("未上线关卡", disabledLevels, disabledLevels > 0 ? "danger" : "normal"),
            todoItem("应用版本记录", versions, "normal")
        );
    }

    private Map<String, Object> todoItem(String label, long count, String level) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("count", count);
        item.put("level", level);
        return item;
    }

    private List<Map<String, Object>> recentActivities() {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreateTime)
                .last("LIMIT 5"))
            .stream()
            .sorted(Comparator.comparing(OperationLog::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(log -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("time", log.getCreateTime() == null ? "--:--" : log.getCreateTime().format(ACTIVITY_TIME));
                item.put("title", Objects.toString(log.getAction(), "后台操作"));
                item.put("desc", Objects.toString(log.getModule(), "系统"));
                return item;
            })
            .toList();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private record DailyLearningRow(LocalDate statDate, long answers, long passed) {}
}
