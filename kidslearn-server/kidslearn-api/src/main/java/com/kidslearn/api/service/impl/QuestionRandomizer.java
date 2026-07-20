package com.kidslearn.api.service.impl;

import com.kidslearn.api.entity.QuestionOption;
import com.kidslearn.common.util.RichContentUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class QuestionRandomizer {

    private QuestionRandomizer() {
    }

    static <T> List<T> shuffledCopy(List<T> source, Random random) {
        List<T> copy = new ArrayList<>(source);
        Collections.shuffle(copy, random);
        return copy;
    }

    static List<Map<String, String>> toRandomizedOptions(List<QuestionOption> options, Random random) {
        return toRandomizedOptions(options, random, null);
    }

    static List<Map<String, String>> toRandomizedOptions(List<QuestionOption> options, Random random, Integer questionType) {
        // 判断题处理
        if (questionType != null && questionType == 2) {
            if (options == null || options.isEmpty()) {
                // 数据库没有选项记录，自动生成"正确"/"错误"
                options = new ArrayList<>();
                QuestionOption correct = new QuestionOption();
                correct.setOptionLabel("");
                correct.setOptionContent("正确");
                correct.setIsCorrect(1);
                correct.setSortOrder(1);
                options.add(correct);

                QuestionOption wrong = new QuestionOption();
                wrong.setOptionLabel("");
                wrong.setOptionContent("错误");
                wrong.setIsCorrect(0);
                wrong.setSortOrder(2);
                options.add(wrong);
            } else {
                // 数据库有选项（可能是错误的 A/B/C/D），强制标准化为"正确"/"错误"
                for (QuestionOption opt : options) {
                    boolean isCorrect = opt.getIsCorrect() != null && opt.getIsCorrect() == 1;
                    opt.setOptionLabel("");
                    opt.setOptionContent(isCorrect ? "正确" : "错误");
                }
            }
        }

        List<QuestionOption> shuffled = shuffledCopy(options, random);
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i++) {
            QuestionOption option = shuffled.get(i);
            Map<String, String> opt = new HashMap<>();

            String optionLabel = option.getOptionLabel();
            String optionText = RichContentUtil.toPlainText(option.getOptionContent());

            // 判断选项类型
            boolean hasCustomLabel = optionLabel != null && !optionLabel.isEmpty();
            boolean isSingleLetter = hasCustomLabel && optionLabel.matches("^[A-Z]$");

            if (hasCustomLabel && !isSingleLetter) {
                // 连线题：optionLabel 是有意义的内容（如"太阳"、"月亮"）
                opt.put("optionLabel", optionLabel);
                opt.put("answerValue", optionLabel);
            } else if (hasCustomLabel && isSingleLetter) {
                // 选择题：optionLabel 是 A/B/C/D
                opt.put("optionLabel", optionLabel);
                opt.put("answerValue", optionLabel);
            } else {
                // 判断题或其他：optionLabel 为空，使用 optionContent 作为显示和提交值
                opt.put("optionLabel", optionText);
                opt.put("answerValue", optionText);
            }

            opt.put("optionContent", option.getOptionContent());
            opt.put("optionText", optionText);
            opt.put("optionSpeechText", RichContentUtil.toSpeechText(option.getOptionContent()));
            opt.put("optionAudioUrl", RichContentUtil.toSpeechAudioUrl(option.getOptionContent()));
            result.add(opt);
        }
        return result;
    }
}
