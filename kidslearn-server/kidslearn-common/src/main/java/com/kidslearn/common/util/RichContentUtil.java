package com.kidslearn.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class RichContentUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String RICH_TEXT_TYPE = "richText";
    private static final String IMAGE_PLACEHOLDER = "[image]";

    private RichContentUtil() {
    }

    public static boolean isRichContent(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(value);
            return root.isObject() && RICH_TEXT_TYPE.equals(root.path("type").asText());
        } catch (Exception e) {
            return false;
        }
    }

    public static String toPlainText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(value);
            if (!root.isObject() || !RICH_TEXT_TYPE.equals(root.path("type").asText())) {
                return value;
            }
            JsonNode blocks = root.path("blocks");
            if (!blocks.isArray()) {
                return "";
            }
            StringBuilder plainText = new StringBuilder();
            for (JsonNode block : blocks) {
                String blockText = blockToPlainText(block);
                if (blockText.isBlank()) {
                    continue;
                }
                if (!plainText.isEmpty()) {
                    plainText.append(' ');
                }
                plainText.append(blockText);
            }
            return plainText.toString();
        } catch (Exception e) {
            return value;
        }
    }

    public static String toSpeechText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(value);
            if (!root.isObject() || !RICH_TEXT_TYPE.equals(root.path("type").asText())) {
                return value;
            }
            String configuredSpeechText = root.path("speech").path("text").asText("");
            if (!configuredSpeechText.isBlank()) {
                return configuredSpeechText;
            }
            JsonNode blocks = root.path("blocks");
            if (!blocks.isArray()) {
                return "";
            }
            StringBuilder speechText = new StringBuilder();
            for (JsonNode block : blocks) {
                String text = blockToSpeechText(block);
                if (text.isBlank()) {
                    continue;
                }
                if (!speechText.isEmpty()) {
                    speechText.append(' ');
                }
                speechText.append(text);
            }
            return speechText.toString();
        } catch (Exception e) {
            return value;
        }
    }

    public static String toSpeechAudioUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(value);
            if (!root.isObject() || !RICH_TEXT_TYPE.equals(root.path("type").asText())) {
                return "";
            }
            return root.path("speech").path("audioUrl").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    public static String withSpeech(String value, String speechText, String audioUrl) {
        try {
            ObjectNode root = toRichTextRoot(value);
            ObjectNode speech = OBJECT_MAPPER.createObjectNode();
            speech.put("text", speechText == null ? "" : speechText);
            if (audioUrl != null && !audioUrl.isBlank()) {
                speech.put("audioUrl", audioUrl);
            }
            root.set("speech", speech);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }

    public static String summary(String value, int maxLength) {
        String plainText = toPlainText(value);
        if (maxLength <= 0 || plainText.length() <= maxLength) {
            return plainText;
        }
        if (maxLength <= 3) {
            return plainText.substring(0, maxLength);
        }
        return plainText.substring(0, maxLength - 3) + "...";
    }

    private static String blockToPlainText(JsonNode block) {
        String type = block.path("type").asText();
        if ("paragraph".equals(type)) {
            return block.path("text").asText("");
        }
        if ("image".equals(type)) {
            String alt = block.path("alt").asText("");
            return alt.isBlank() ? IMAGE_PLACEHOLDER : alt;
        }
        return "";
    }

    private static String blockToSpeechText(JsonNode block) {
        String type = block.path("type").asText();
        if ("paragraph".equals(type)) {
            return block.path("text").asText("");
        }
        if ("image".equals(type)) {
            return block.path("alt").asText("");
        }
        return "";
    }

    private static ObjectNode toRichTextRoot(String value) throws Exception {
        if (value != null && !value.isBlank()) {
            try {
                JsonNode parsed = OBJECT_MAPPER.readTree(value);
                if (parsed.isObject() && RICH_TEXT_TYPE.equals(parsed.path("type").asText())) {
                    ObjectNode copy = parsed.deepCopy();
                    if (!copy.path("blocks").isArray()) {
                        copy.set("blocks", paragraphBlocks(""));
                    }
                    if (!copy.has("version")) {
                        copy.put("version", 1);
                    }
                    return copy;
                }
            } catch (Exception ignored) {
                // Legacy plain text.
            }
        }

        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("type", RICH_TEXT_TYPE);
        root.put("version", 1);
        root.set("blocks", paragraphBlocks(value == null ? "" : value));
        return root;
    }

    private static ArrayNode paragraphBlocks(String text) {
        ArrayNode blocks = OBJECT_MAPPER.createArrayNode();
        ObjectNode paragraph = OBJECT_MAPPER.createObjectNode();
        paragraph.put("type", "paragraph");
        paragraph.put("text", text == null ? "" : text);
        blocks.add(paragraph);
        return blocks;
    }
}
