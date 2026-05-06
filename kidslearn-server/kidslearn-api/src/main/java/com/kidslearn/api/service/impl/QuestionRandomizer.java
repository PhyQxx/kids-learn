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
        List<QuestionOption> shuffled = shuffledCopy(options, random);
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i++) {
            QuestionOption option = shuffled.get(i);
            Map<String, String> opt = new HashMap<>();
            opt.put("optionLabel", String.valueOf((char) ('A' + i)));
            opt.put("answerValue", option.getOptionLabel());
            opt.put("optionContent", option.getOptionContent());
            opt.put("optionText", RichContentUtil.toPlainText(option.getOptionContent()));
            opt.put("optionSpeechText", RichContentUtil.toSpeechText(option.getOptionContent()));
            opt.put("optionAudioUrl", RichContentUtil.toSpeechAudioUrl(option.getOptionContent()));
            result.add(opt);
        }
        return result;
    }
}
