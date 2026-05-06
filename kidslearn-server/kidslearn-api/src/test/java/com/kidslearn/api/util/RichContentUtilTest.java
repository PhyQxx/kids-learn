package com.kidslearn.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kidslearn.common.util.RichContentUtil;
import org.junit.jupiter.api.Test;

class RichContentUtilTest {

    @Test
    void treatsLegacyPlainTextAsPlainText() {
        assertEquals("Plain question", RichContentUtil.toPlainText("Plain question"));
    }

    @Test
    void extractsTextAndImageAltFromRichContent() {
        String richJson = "{\"type\":\"richText\",\"version\":1,\"blocks\":["
            + "{\"type\":\"paragraph\",\"text\":\"Look\"},"
            + "{\"type\":\"image\",\"url\":\"https://example.com/a.png\",\"alt\":\"Image alt\"}"
            + "]}";

        assertEquals("Look Image alt", RichContentUtil.toPlainText(richJson));
    }

    @Test
    void extractsSpeechTextAndAudioUrlFromRichContent() {
        String richJson = "{\"type\":\"richText\",\"version\":1,"
            + "\"speech\":{\"text\":\"Look and answer\",\"audioUrl\":\"https://example.com/q.mp3\"},"
            + "\"blocks\":["
            + "{\"type\":\"paragraph\",\"text\":\"Look\"},"
            + "{\"type\":\"image\",\"url\":\"https://example.com/a.png\",\"alt\":\"Image alt\"}"
            + "]}";

        assertEquals("Look and answer", RichContentUtil.toSpeechText(richJson));
        assertEquals("https://example.com/q.mp3", RichContentUtil.toSpeechAudioUrl(richJson));
    }

    @Test
    void speechTextUsesImageAltWhenNoSpeechTextIsConfigured() {
        String richJson = "{\"type\":\"richText\",\"version\":1,\"blocks\":["
            + "{\"type\":\"paragraph\",\"text\":\"Look\"},"
            + "{\"type\":\"image\",\"url\":\"https://example.com/a.png\",\"alt\":\"Image alt\"}"
            + "]}";

        assertEquals("Look Image alt", RichContentUtil.toSpeechText(richJson));
    }

    @Test
    void updatesSpeechMetadataWithoutChangingRichBlocks() {
        String richJson = "{\"type\":\"richText\",\"version\":1,\"blocks\":["
            + "{\"type\":\"paragraph\",\"text\":\"Look\"},"
            + "{\"type\":\"image\",\"url\":\"https://example.com/a.png\",\"alt\":\"Image alt\"}"
            + "]}";

        String updated = RichContentUtil.withSpeech(richJson, "Read this", "https://example.com/q.wav");

        assertEquals("Look Image alt", RichContentUtil.toPlainText(updated));
        assertEquals("Read this", RichContentUtil.toSpeechText(updated));
        assertEquals("https://example.com/q.wav", RichContentUtil.toSpeechAudioUrl(updated));
    }

    @Test
    void wrapsLegacyPlainTextWhenUpdatingSpeechMetadata() {
        String updated = RichContentUtil.withSpeech("Plain question", "Read plain", "https://example.com/plain.wav");

        assertEquals("Plain question", RichContentUtil.toPlainText(updated));
        assertEquals("Read plain", RichContentUtil.toSpeechText(updated));
        assertEquals("https://example.com/plain.wav", RichContentUtil.toSpeechAudioUrl(updated));
    }

    @Test
    void keepsMalformedJsonAsPlainText() {
        assertEquals("{bad json", RichContentUtil.toPlainText("{bad json"));
    }

    @Test
    void usesImagePlaceholderWhenImageHasNoAlt() {
        String richJson = "{\"type\":\"richText\",\"version\":1,\"blocks\":["
            + "{\"type\":\"image\",\"url\":\"https://example.com/a.png\"}"
            + "]}";

        assertEquals("[image]", RichContentUtil.toPlainText(richJson));
    }

    @Test
    void truncatesSummaryWithoutCuttingShortValues() {
        assertEquals("Short", RichContentUtil.summary("Short", 20));
        assertEquals("Very...", RichContentUtil.summary("Very long value", 7));
    }
}
