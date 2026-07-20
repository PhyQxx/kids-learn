package com.kidslearn.api.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidslearn.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MiMo cloud TTS generator.
 * Calls the MiMo-V2.5 speech synthesis API (OpenAI-compatible chat completions format).
 */
@Slf4j
@Component("mimo")
@RequiredArgsConstructor
class MimoTtsAudioGenerator implements QuestionAudioGenerator {

    private final QuestionAudioProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Path generate(String text) throws IOException {
        Path tempDir = Path.of(properties.getTempDir());
        Files.createDirectories(tempDir);

        String apiKey = properties.getMimoApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("MiMo API key is not configured (question.audio.mimo-api-key)");
        }

        String baseUrl = properties.getMimoBaseUrl();
        String url = baseUrl.endsWith("/")
            ? baseUrl + "chat/completions"
            : baseUrl + "/chat/completions";

        // Build request body following MiMo TTS API spec
        Map<String, Object> body = Map.of(
            "model", properties.getMimoModel(),
            "messages", List.of(
                Map.of("role", "assistant", "content", text)
            ),
            "audio", Map.of(
                "format", "wav",
                "voice", properties.getMimoVoice()
            )
        );

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BusinessException("Failed to serialize MiMo TTS request", e);
        }

        log.info("MiMo TTS request url={} model={} voice={} textLength={}",
            url, properties.getMimoModel(), properties.getMimoVoice(), text.length());

        HttpResponse response = HttpRequest.post(url)
            .header("Content-Type", "application/json")
            .header("api-key", apiKey)
            .body(jsonBody)
            .timeout((int) properties.getTimeout().toMillis())
            .execute();

        String responseBody = response.body();
        log.info("MiMo TTS response status={}", response.getStatus());

        if (!response.isOk()) {
            log.error("MiMo TTS request failed, status={} body={}", response.getStatus(), responseBody);
            throw new BusinessException("MiMo TTS request failed with status " + response.getStatus());
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new BusinessException("MiMo TTS response has no choices");
            }

            JsonNode audioData = choices.get(0)
                .path("message")
                .path("audio")
                .path("data");

            if (audioData.isMissingNode() || audioData.asText().isBlank()) {
                throw new BusinessException("MiMo TTS response has no audio data");
            }

            byte[] wavBytes = Base64.getDecoder().decode(audioData.asText());
            Path output = Files.createTempFile(tempDir, "mimo-audio-", ".wav");
            Files.write(output, wavBytes);
            return output;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse MiMo TTS response: {}", responseBody, e);
            throw new BusinessException("Failed to parse MiMo TTS response", e);
        }
    }
}
