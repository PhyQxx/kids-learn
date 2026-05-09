package com.kidslearn.api.generator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class SeedQuestionGenerator {

    private static final String[] SUBJECTS = {"MATH", "CHINESE", "ENGLISH", "LOGIC", "SCIENCE"};
    private static final long[] SUBJECT_IDS = {1L, 2L, 3L, 4L, 5L}; // 假设1-5分别是数语外逻科
    private static final long[] GRADE_IDS = {1L, 2L, 3L, 4L, 5L, 6L}; // 假设有6个年级
    private static final Random random = new Random();
    private static long questionIdCounter = 1000;

    public static void main(String[] args) {
        String filePath = "seed_question_bank.sql";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("-- 自动生成的全量题库种子数据 (30道/学科/年级/题型)\n");
            writer.write("-- 为避免 Collation 问题，本脚本使用直接的 ID 插入\n");
            writer.write("SET NAMES utf8mb4;\n");
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n");
            writer.write("TRUNCATE TABLE `question`;\n");
            writer.write("TRUNCATE TABLE `question_option`;\n\n");

            for (int sIdx = 0; sIdx < SUBJECTS.length; sIdx++) {
                String subjectCode = SUBJECTS[sIdx];
                long subjectId = SUBJECT_IDS[sIdx];

                for (long gradeId : GRADE_IDS) {
                    for (int type = 1; type <= 5; type++) {
                        for (int i = 0; i < 30; i++) {
                            generateQuestion(writer, subjectCode, subjectId, gradeId, type);
                        }
                    }
                }
            }
            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
            System.out.println("成功生成 SQL 文件: " + filePath + "，总题数: " + (questionIdCounter - 1000));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateQuestion(BufferedWriter writer, String subjectCode, long subjectId, long gradeId, int type) throws IOException {
        long qId = questionIdCounter++;
        String content = generateContent(subjectCode, gradeId, type);
        String analysis = "{\"text\": \"这是系统自动生成的详细解析，帮助孩子理解为什么这么做。\"}";
        int score = 10;
        int timeLimit = 60;

        writer.write(String.format("INSERT INTO `question` (`id`, `subject_id`, `grade_level_id`, `question_type`, `question_content`, `score`, `time_limit`, `analysis`, `sort_order`) VALUES (%d, %d, %d, %d, '%s', %d, %d, '%s', 1);\n",
                qId, subjectId, gradeId, type, content, score, timeLimit, analysis));

        generateOptions(writer, qId, subjectCode, type);
        writer.write("\n");
    }

    private static String generateContent(String subject, long grade, int type) {
        String text = "";
        switch (subject) {
            case "MATH":
                int a = random.nextInt((int)(grade * 10)) + 1;
                int b = random.nextInt((int)(grade * 10)) + 1;
                if (type == 1) text = a + " + " + b + " = ?";
                else if (type == 2) text = a + " + " + b + " = " + (a + b) + "，这个算式对吗？";
                else if (type == 3) text = a + " + " + b + " = ____";
                else if (type == 4) text = "请将下列数字从小到大排序：";
                else if (type == 5) text = "请将算式与正确的得数连线：";
                break;
            case "CHINESE":
                if (type == 1) text = "下列哪个字是正确的？";
                else if (type == 2) text = "《静夜思》是李白写的，对吗？";
                else if (type == 3) text = "春眠不觉____，处处闻啼鸟。";
                else if (type == 4) text = "请将下列词语排列成一句通顺的话：";
                else if (type == 5) text = "请将诗句的上下半句连线：";
                break;
            case "ENGLISH":
                if (type == 1) text = "Choose the correct word for '苹果':";
                else if (type == 2) text = "'Apple' means '香蕉', is it true?";
                else if (type == 3) text = "I ____ a student. (填入系动词)";
                else if (type == 4) text = "Please order the words to form a sentence:";
                else if (type == 5) text = "Match the words with their meanings:";
                break;
            case "LOGIC":
                if (type == 1) text = "下面哪个图形是正方形？";
                else if (type == 2) text = "太阳从西边升起，对吗？";
                else if (type == 3) text = "找规律：1, 3, 5, ____, 9";
                else if (type == 4) text = "请按照时间先后顺序排列：";
                else if (type == 5) text = "请将有逻辑关联的物品连线：";
                break;
            case "SCIENCE":
                if (type == 1) text = "下列哪个动物生活在水里？";
                else if (type == 2) text = "水在100摄氏度时会沸腾，对吗？";
                else if (type == 3) text = "地球唯一的天然卫星是____。";
                else if (type == 4) text = "请将植物生长的过程排序：";
                else if (type == 5) text = "请将动物与其栖息地连线：";
                break;
        }
        return "{\"text\": \"" + text + "\"}";
    }

    private static void generateOptions(BufferedWriter writer, long qId, String subject, int type) throws IOException {
        if (type == 1) {
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, 'A', '{\"text\":\"%s\"}', 1, 1);\n", qId, "正确选项"));
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, 'B', '{\"text\":\"%s\"}', 0, 2);\n", qId, "错误选项1"));
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, 'C', '{\"text\":\"%s\"}', 0, 3);\n", qId, "错误选项2"));
        } else if (type == 2) {
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '', '正确', 1, 1);\n", qId));
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '', '错误', 0, 2);\n", qId));
        } else if (type == 3) {
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '', '%s', 1, 1);\n", qId, "正确答案"));
        } else if (type == 4) {
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '', '{\"text\":\"第一步\"}', 1, 1);\n", qId));
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '', '{\"text\":\"第二步\"}', 1, 2);\n", qId));
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '', '{\"text\":\"第三步\"}', 1, 3);\n", qId));
        } else if (type == 5) {
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '左侧A', '{\"text\":\"右侧A\"}', 1, 1);\n", qId));
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '左侧B', '{\"text\":\"右侧B\"}', 1, 2);\n", qId));
            writer.write(String.format("INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES (%d, '左侧C', '{\"text\":\"右侧C\"}', 1, 3);\n", qId));
        }
    }
}
