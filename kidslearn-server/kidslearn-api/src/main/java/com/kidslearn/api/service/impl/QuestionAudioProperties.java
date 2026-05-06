package com.kidslearn.api.service.impl;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "question.audio")
public class QuestionAudioProperties {

    private String ttsCommand = "/opt/moss-tts/bin/moss-tts-nano";
    private String backend = "onnx";
    private String voice = "Junhao";
    private String tempDir = System.getProperty("java.io.tmpdir");
    private Duration timeout = Duration.ofMinutes(5);
}
