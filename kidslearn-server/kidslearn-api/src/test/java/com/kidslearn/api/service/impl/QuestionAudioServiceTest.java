package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.entity.Question;
import com.kidslearn.api.mapper.QuestionMapper;
import com.kidslearn.common.ftp.FtpTool;
import com.kidslearn.common.util.RichContentUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class QuestionAudioServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesAudioUploadsToFtpAndUpdatesQuestionSpeechMetadata() throws IOException {
        QuestionMapper questionMapper = Mockito.mock(QuestionMapper.class);
        FtpTool ftpTool = Mockito.mock(FtpTool.class);
        FakeAudioGenerator audioGenerator = new FakeAudioGenerator(tempDir);
        QuestionAudioProperties properties = Mockito.mock(QuestionAudioProperties.class);
        when(properties.getEngine()).thenReturn("fake");
        QuestionAudioService service = new QuestionAudioService(questionMapper, ftpTool, properties,
                Map.of("fake", audioGenerator));

        Question question = new Question();
        question.setId(12L);
        question.setQuestionContent("{\"type\":\"richText\",\"version\":1,\"blocks\":[{\"type\":\"paragraph\",\"text\":\"旧题目\"}]}");
        when(questionMapper.selectById(12L)).thenReturn(question);
        when(ftpTool.upload(eq("/question/audio/admin/question-12"), any(), any(InputStream.class))).thenReturn("q.wav");
        when(ftpTool.buildPublicUrl("/question/audio/admin/question-12", "q.wav")).thenReturn("https://cdn.example.com/q.wav");

        Map<String, String> result = service.generateQuestionAudio(12L, "新的朗读文本", null);

        assertEquals("https://cdn.example.com/q.wav", result.get("audioUrl"));
        assertEquals("新的朗读文本", audioGenerator.receivedText);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).updateById(captor.capture());
        String updatedContent = captor.getValue().getQuestionContent();
        assertEquals("新的朗读文本", RichContentUtil.toSpeechText(updatedContent));
        assertEquals("https://cdn.example.com/q.wav", RichContentUtil.toSpeechAudioUrl(updatedContent));
        assertEquals("旧题目", RichContentUtil.toPlainText(updatedContent));
        assertTrue(Files.notExists(audioGenerator.generatedFile));
    }

    private static class FakeAudioGenerator implements QuestionAudioGenerator {
        private final Path tempDir;
        private String receivedText;
        private Path generatedFile;

        private FakeAudioGenerator(Path tempDir) {
            this.tempDir = tempDir;
        }

        @Override
        public Path generate(String text) throws IOException {
            receivedText = text;
            generatedFile = Files.createTempFile(tempDir, "question-audio-", ".wav");
            Files.write(generatedFile, new byte[] { 1, 2, 3 });
            return generatedFile;
        }
    }
}
