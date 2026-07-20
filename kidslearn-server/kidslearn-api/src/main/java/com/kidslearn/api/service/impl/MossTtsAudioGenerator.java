package com.kidslearn.api.service.impl;

import com.kidslearn.common.exception.BusinessException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("moss")
@RequiredArgsConstructor
public class MossTtsAudioGenerator implements QuestionAudioGenerator {

    private final QuestionAudioProperties properties;

    @Override
    public Path generate(String text) throws IOException {
        Path tempDir = Path.of(properties.getTempDir());
        Files.createDirectories(tempDir);
        Path output = Files.createTempFile(tempDir, "question-audio-", ".wav");

        List<String> command = List.of(
            properties.getTtsCommand(),
            "generate",
            "--backend",
            properties.getBackend(),
            "--voice",
            properties.getVoice(),
            "--text",
            text,
            "--output",
            output.toString()
        );

        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
        ByteArrayOutputStream processOutput = new ByteArrayOutputStream();
        Thread outputReader = new Thread(() -> copyProcessOutput(process.getInputStream(), processOutput));
        outputReader.setDaemon(true);
        outputReader.start();

        try {
            boolean finished = process.waitFor(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
            outputReader.join(1_000);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("TTS generation timed out");
            }
            if (process.exitValue() != 0 || !Files.exists(output) || Files.size(output) == 0) {
                throw new BusinessException("TTS generation failed: " + processOutput.toString(StandardCharsets.UTF_8));
            }
            return output;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new BusinessException("TTS generation interrupted");
        }
    }

    private void copyProcessOutput(InputStream inputStream, ByteArrayOutputStream outputStream) {
        try (inputStream; outputStream) {
            inputStream.transferTo(outputStream);
        } catch (IOException ignored) {
            // Process output is only used for diagnostics.
        }
    }
}
