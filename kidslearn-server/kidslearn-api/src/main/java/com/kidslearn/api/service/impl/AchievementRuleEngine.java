package com.kidslearn.api.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class AchievementRuleEngine {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AchievementRuleEngine() {
    }

    record ProgressSnapshot(
        int completedLevels,
        int threeStarLevels,
        int perfectLevels,
        int mathCompletedLevels,
        int mathPerfectLevels,
        int learnedSubjects,
        int collectedStickers,
        int streakDays,
        int bestRank
    ) {
    }

    record RewardItem(String type, Long itemId, int quantity) {
    }

    static int resolveTarget(String conditionJson, String achieveCode, String achieveName) {
        Map<String, Object> condition = parseObject(conditionJson);
        String type = normalizeType(condition.get("type"));

        if ("RANK_TOP".equals(type) || "PERFECT_SCORE".equals(type) || "MATH_PERFECT".equals(type)) {
            return 1;
        }

        int target = firstInt(condition,
            "target", "targetValue", "count", "value", "levelCount", "starCount", "stickerCount",
            "subjectCount", "days", "rank");
        if (target > 0) {
            return target;
        }

        String code = safeLower(achieveCode);
        String name = achieveName == null ? "" : achieveName;
        if (code.contains("first") || name.contains("首次") || name.contains("初次")) return 1;
        if (code.contains("streak") || code.contains("checkin") || name.contains("连续")) return 7;
        if (isPerfectAchievement(code, name)) return 1;
        if (code.contains("star") || name.contains("满星") || name.contains("三星")) return 10;
        if (code.contains("math") || name.contains("数学")) return 50;
        if (code.contains("subject") || name.contains("全科")) return 6;
        if (code.contains("sticker") || name.contains("贴纸") || name.contains("收集")) return 100;
        return 1;
    }

    static int resolveCurrent(String conditionJson, String achieveCode, String achieveName, ProgressSnapshot snapshot) {
        Map<String, Object> condition = parseObject(conditionJson);
        String type = normalizeType(condition.get("type"));

        return switch (type) {
            case "STICKER", "STICKER_COUNT" -> snapshot.collectedStickers();
            case "SUBJECT_COUNT" -> snapshot.learnedSubjects();
            case "THREE_STAR" -> snapshot.threeStarLevels();
            case "PERFECT_SCORE" -> snapshot.perfectLevels();
            case "MATH_LEVEL" -> snapshot.mathCompletedLevels();
            case "MATH_PERFECT" -> snapshot.mathPerfectLevels();
            case "STREAK_DAYS" -> snapshot.streakDays();
            case "RANK_TOP" -> {
                int requiredRank = Math.max(1, firstInt(condition, "rank", "target", "value"));
                yield snapshot.bestRank() > 0 && snapshot.bestRank() <= requiredRank ? 1 : 0;
            }
            case "COMPLETE_LEVEL" -> snapshot.completedLevels();
            default -> resolveCurrentByLegacyText(achieveCode, achieveName, snapshot);
        };
    }

    static int resolveGoldReward(String rewardJson, int defaultGold) {
        Integer gold = resolveRewardItems(rewardJson).stream()
            .filter(item -> "GOLD".equals(item.type()))
            .map(RewardItem::quantity)
            .findFirst()
            .orElse(null);
        return gold != null && gold > 0 ? gold : defaultGold;
    }

    static List<RewardItem> resolveRewardItems(String rewardJson) {
        List<RewardItem> rewards = new ArrayList<>();
        collectRewardItems(parseJson(rewardJson), rewards);
        return rewards;
    }

    private static int resolveCurrentByLegacyText(String achieveCode, String achieveName, ProgressSnapshot snapshot) {
        String code = safeLower(achieveCode);
        String name = achieveName == null ? "" : achieveName;
        if (code.contains("sticker") || name.contains("贴纸") || name.contains("收集")) {
            return snapshot.collectedStickers();
        }
        if (code.contains("subject") || name.contains("全科")) {
            return snapshot.learnedSubjects();
        }
        if (isMathPerfectAchievement(code, name)) {
            return snapshot.mathPerfectLevels();
        }
        if (isPerfectAchievement(code, name)) {
            return snapshot.perfectLevels();
        }
        if (code.contains("math") || name.contains("数学")) {
            return snapshot.mathCompletedLevels();
        }
        if (code.contains("star") || name.contains("满星") || name.contains("三星")) {
            return snapshot.threeStarLevels();
        }
        if (code.contains("streak") || code.contains("checkin") || name.contains("连续")) {
            return snapshot.streakDays();
        }
        return snapshot.completedLevels();
    }

    private static void collectRewardItems(Object node, List<RewardItem> rewards) {
        if (node instanceof Map<?, ?> map) {
            String type = normalizeType(map.get("type"));
            if (!type.isBlank()) {
                int value = firstInt(map, "value", "quantity", "amount", "count");
                long itemId = firstLong(map, "itemId", "rewardItemId", "id", "stickerId", "titleId");
                rewards.add(new RewardItem(type, itemId > 0 ? itemId : null, Math.max(1, value)));
            }
            collectRewardItems(map.get("rewards"), rewards);
            collectRewardItems(map.get("items"), rewards);
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                collectRewardItems(item, rewards);
            }
        }
    }

    private static Map<String, Object> parseObject(String json) {
        Object parsed = parseJson(json);
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    private static Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Object>() {});
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private static int firstInt(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            int number = toInt(value);
            if (number > 0) {
                return number;
            }
        }
        return 0;
    }

    private static long firstLong(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            long number = toLong(value);
            if (number > 0) {
                return number;
            }
        }
        return 0;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String normalizeType(Object value) {
        if (value == null) {
            return "";
        }
        String type = value.toString().trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return switch (type) {
            case "GOLD", "COIN", "COINS" -> "GOLD";
            case "EXP", "EXPERIENCE" -> "EXP";
            case "STICKER", "STICKERS" -> "STICKER";
            case "TITLE", "TITLES" -> "TITLE";
            case "DIAMOND", "DIAMONDS" -> "DIAMOND";
            case "COMPLETE_LEVEL", "COMPLETED_LEVELS", "LEVEL_COUNT", "PASS_LEVEL" -> "COMPLETE_LEVEL";
            case "THREE_STAR", "THREE_STAR_LEVEL", "THREE_STAR_COUNT", "STAR_COUNT" -> "THREE_STAR";
            case "PERFECT", "PERFECT_SCORE", "FULL_SCORE", "NO_WRONG", "ZERO_WRONG" -> "PERFECT_SCORE";
            case "MATH", "MATH_LEVEL", "MATH_LEVEL_COUNT", "MATH_COMPLETE", "MATH_COMPLETED" -> "MATH_LEVEL";
            case "MATH_PERFECT", "MATH_FULL_SCORE", "MATH_NO_WRONG" -> "MATH_PERFECT";
            case "STREAK", "STREAK_DAY", "STREAK_DAYS", "CONSECUTIVE_DAYS", "CHECKIN_STREAK" -> "STREAK_DAYS";
            case "SUBJECT", "SUBJECT_COUNT", "LEARNED_SUBJECTS" -> "SUBJECT_COUNT";
            case "STICKER_COUNT", "COLLECT_STICKER", "COLLECTED_STICKERS" -> "STICKER_COUNT";
            case "RANK", "RANK_TOP", "LEADERBOARD_TOP" -> "RANK_TOP";
            default -> type;
        };
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean isPerfectAchievement(String code, String name) {
        return code.contains("perfect") ||
            code.contains("full_score") ||
            code.contains("no_wrong") ||
            name.contains("满分") ||
            name.contains("全对");
    }

    private static boolean isMathPerfectAchievement(String code, String name) {
        return isPerfectAchievement(code, name) &&
            (code.contains("math") || name.contains("数学"));
    }
}
