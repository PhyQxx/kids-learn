package com.kidslearn.api.service.impl;

import com.kidslearn.api.entity.Question;
import com.kidslearn.api.mapper.QuestionMapper;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.ftp.FtpTool;
import com.kidslearn.common.util.RichContentUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionAudioService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final QuestionMapper questionMapper;
    private final FtpTool ftpTool;
    private final QuestionAudioProperties properties;
    private final Map<String, QuestionAudioGenerator> generators;

    public Map<String, String> generateQuestionAudio(Long questionId, String requestedSpeechText, String engine) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("Question not found");
        }

        String speechText = chooseSpeechText(question.getQuestionContent(), requestedSpeechText);

        String selectedEngine = (engine != null && !engine.isBlank()) ? engine : properties.getEngine();
        QuestionAudioGenerator generator = generators.get(selectedEngine);
        if (generator == null) {
            throw new BusinessException("Unknown TTS engine: " + selectedEngine + ", available: " + generators.keySet());
        }

        Path audioFile = null;
        try {
            audioFile = generator.generate(speechText);
            String serviceDir = "/question/audio/admin/question-" + questionId;
            String fileName = FILE_TIME.format(LocalDateTime.now()) + ".wav";
            try (InputStream inputStream = Files.newInputStream(audioFile)) {
                fileName = ftpTool.upload(serviceDir, fileName, inputStream);
            }
            String audioUrl = ftpTool.buildPublicUrl(serviceDir, fileName);
            question.setQuestionContent(RichContentUtil.withSpeech(question.getQuestionContent(), speechText, audioUrl));
            questionMapper.updateById(question);
            return Map.of(
                "audioUrl", audioUrl,
                "speechText", speechText
            );
        } catch (IOException e) {
            throw new BusinessException("Question audio generation failed");
        } finally {
            if (audioFile != null) {
                try {
                    Files.deleteIfExists(audioFile);
                } catch (IOException ignored) {
                    // Temporary file cleanup failure should not mask the main result.
                }
            }
        }
    }

    private String chooseSpeechText(String questionContent, String requestedSpeechText) {
        if (requestedSpeechText != null && !requestedSpeechText.isBlank()) {
            return requestedSpeechText.trim();
        }
        String speechText = RichContentUtil.toSpeechText(questionContent);
        if (speechText.isBlank()) {
            speechText = RichContentUtil.toPlainText(questionContent);
        }
        if (speechText.isBlank()) {
            throw new BusinessException("Question speech text is empty");
        }
        return speechText;
    }
}
