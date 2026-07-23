package com.kidslearn.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.mapper.AppConfigMapper;
import com.kidslearn.common.ftp.FtpTool;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AiServiceParentSummaryTest {

    @Test
    void logsAiRequestAndResponseWithoutApiKey() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = """
                {"choices":[{"message":{"content":"{\\"summary\\":\\"今天状态不错\\",\\"highlights\\":[\\"完成了练习\\"],\\"concerns\\":[],\\"suggestions\\":[\\"保持节奏\\"]}"}}]}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            AppConfigMapper appConfigMapper = mock(AppConfigMapper.class);
            when(appConfigMapper.selectList(any())).thenReturn(List.of(
                config("ai.provider", "openai"),
                config("ai.openai.api_key", "secret-key-should-not-log"),
                config("ai.openai.base_url", "http://127.0.0.1:" + server.getAddress().getPort()),
                config("ai.openai.model", "test-model"),
                config("ai.timeout", "3")
            ));
            AiService aiService = new AiService(appConfigMapper, mock(FtpTool.class));
            Logger logger = (Logger) LoggerFactory.getLogger(AiService.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                Map<String, Object> summary = aiService.generateParentSummary(
                    Map.of("today", Map.of("learnMinutes", 20)),
                    Map.of()
                );

                assertEquals("今天状态不错", summary.get("summary"));
                String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
                assertTrue(logs.contains("AI chat request"));
                assertTrue(logs.contains("learnMinutes"));
                assertTrue(logs.contains("AI chat response"));
                assertTrue(logs.contains("今天状态不错"));
                assertFalse(logs.contains("secret-key-should-not-log"));
            } finally {
                logger.detachAppender(appender);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void usesReasoningContentWhenLengthFinishHasNoAssistantContent() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = """
                {"choices":[{"finish_reason":"length","message":{"content":"","role":"assistant","reasoning_content":"一直在思考但没有输出JSON"}}]}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            AppConfigMapper appConfigMapper = mock(AppConfigMapper.class);
            when(appConfigMapper.selectList(any())).thenReturn(List.of(
                config("ai.provider", "custom"),
                config("ai.custom.api_key", "test-key"),
                config("ai.custom.base_url", "http://127.0.0.1:" + server.getAddress().getPort()),
                config("ai.custom.model", "mimo-v2.5-pro"),
                config("ai.timeout", "3")
            ));
            AiService aiService = new AiService(appConfigMapper, mock(FtpTool.class));
            Logger logger = (Logger) LoggerFactory.getLogger(AiService.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                Map<String, Object> summary = aiService.generateParentSummary(
                    Map.of(
                        "today", Map.of("learnMinutes", 0, "completedLevels", 0, "accuracy", 0),
                        "stats", Map.of("totalTime", 2, "completedLevels", 2, "accuracy", 18)
                    ),
                    Map.of()
                );

                assertFalse(summary.get("summary").toString().isBlank());
                String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
                assertTrue(logs.contains("maxTokens=1200"));
                assertTrue(logs.contains("AI content empty, using reasoning_content fallback"));
            } finally {
                logger.detachAppender(appender);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void returnsLocalParentSummaryWhenProviderTimesOut() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "{}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            AppConfigMapper appConfigMapper = mock(AppConfigMapper.class);
            when(appConfigMapper.selectList(any())).thenReturn(List.of(
                config("ai.provider", "openai"),
                config("ai.openai.api_key", "test-key"),
                config("ai.openai.base_url", "http://127.0.0.1:" + server.getAddress().getPort()),
                config("ai.openai.model", "test-model"),
                config("ai.timeout", "1")
            ));
            AiService aiService = new AiService(appConfigMapper, mock(FtpTool.class));
            Logger logger = (Logger) LoggerFactory.getLogger(AiService.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                Map<String, Object> summary = aiService.generateParentSummary(
                    Map.of(
                        "today", Map.of("learnMinutes", 35, "completedLevels", 2, "accuracy", 82),
                        "stats", Map.of("totalTime", 180, "completedLevels", 12, "accuracy", 78)
                    ),
                    Map.of()
                );

                assertFalse(summary.get("summary").toString().isBlank());
                assertTrue(((List<?>) summary.get("highlights")).size() > 0);
                assertTrue(((List<?>) summary.get("suggestions")).size() > 0);
                assertTrue(appender.list.stream().anyMatch(event ->
                    event.getLevel() == Level.ERROR &&
                        event.getFormattedMessage().contains("AI parent summary unavailable")
                ));
            } finally {
                logger.detachAppender(appender);
            }
        } finally {
            server.stop(0);
        }
    }

    private static AppConfig config(String key, String value) {
        AppConfig config = new AppConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
