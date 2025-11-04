package com.arth.solabot.adapter.parser.impl;

import com.arth.solabot.adapter.parser.PayloadParser;
import com.arth.solabot.core.bot.dto.ParsedPayloadDTO;
import com.arth.solabot.core.bot.dto.message.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OneBotPayloadParser implements PayloadParser {

    private static final Pattern CQ_REPLY_ID = Pattern.compile("\\[CQ:reply,([^]]+)]");

    public ParsedPayloadDTO parseRawToDTO(JsonNode root, String raw) {
        List<MessageSegment> segments = new ArrayList<>();

        String format = root.path("message_format").asText("string");
        String replyId = null;

        String commandText = null;

        if (root.path("message").isArray()) {
            // ---------- A 方案：array 模式 ----------
            StringBuilder textOnly = new StringBuilder();

            for (JsonNode s : root.path("message")) {
                String type = s.path("type").asText("");
                JsonNode data = s.path("data");

                switch (type) {
                    case "text" -> {
                        String txt = data.path("text").asText("");
                        // 填充 segments
                        TextSegment seg = new TextSegment();
                        seg.setData(Map.of("text", txt));
                        segments.add(seg);
                        // 供命令识别：只拼 text 段
                        textOnly.append(txt);
                    }
                    case "at", "mention" -> {
                        AtSegment seg = new AtSegment();
                        seg.setData(Map.of("qq", data.path("qq").asText("")));
                        segments.add(seg);
                        // 命令识别里忽略
                    }
                    case "image" -> {
                        ImageSegment seg = new ImageSegment();
                        String url = data.hasNonNull("url")
                                ? data.get("url").asText("")
                                : data.path("file").asText("");
                        seg.setData(Map.of("url", url));
                        segments.add(seg);
                    }
                    case "mface" -> {
                        MfaceSegment seg = new MfaceSegment();
                        String url = data.hasNonNull("url")
                                ? data.get("url").asText("")
                                : data.path("file").asText("");
                        seg.setData(Map.of("url", url));
                        segments.add(seg);
                    }
                    case "reply" -> {
                        String id = data.path("id").asText(null);
                        if (id != null && !id.isBlank()) replyId = id;
                        // 命令识别里忽略
                    }
                    default -> {
                        // ...
                    }
                }
            }

            commandText = stripLeadingSpaces(textOnly.toString());
        } else {
            // ---------- B 方案：string (CQ) 模式 ----------
            String cq = root.path("message").asText("");
            replyId = extractReplyIdFromCq(cq);

            // 命令识别：剥掉“行首的 [CQ:reply/**] 和 [CQ:at/**] 段”，再去前导空白
            commandText = cq;
            if (!commandText.isEmpty() && commandText.charAt(0) == '[') {
                commandText = stripLeadingReplyOrAtCq(commandText);
            }
        }

        ParsedPayloadDTO payload = new ParsedPayloadDTO();
        payload.setPostType(root.path("post_type").asText());
        payload.setMessageType(root.path("message_type").asText());
        payload.setSelfId(root.path("self_id").asLong(0));
        payload.setUserId(root.path("user_id").asLong(0));
        payload.setGroupId(root.has("group_id") ? root.path("group_id").asLong() : null);
        payload.setMessageId(root.path("message_id").asLong(0));
        payload.setReplyToMessageId(replyId);
        payload.setTime(root.path("time").asLong(0));
        payload.setRawText(root.path("raw_message").asText(""));
        payload.setSenderRole(root.path("sender").path("role").asText(""));
        payload.setSegments(segments);
        payload.setCommandText(commandText);
        payload.setRawRoot(root);
        payload.setOriginalJsonString(raw);
        return payload;
    }

    /* 仅为 reply 段做一个轻量级 CQ 解析（无需完整 CQ 解析器） */
    private static String extractReplyIdFromCq(String cq) {
        if (cq == null || cq.isEmpty()) return null;
        Matcher m = CQ_REPLY_ID.matcher(cq);
        if (!m.find()) return null;
        String attrs = m.group(1);
        int p = attrs.indexOf("id=");
        if (p < 0) return null;
        int start = p + 3;
        int end = attrs.indexOf(',', start);
        String raw = (end >= 0) ? attrs.substring(start, end) : attrs.substring(start);
        return unescapeCq(raw).trim();
    }

    /* CQ 码反转义 */
    private static String unescapeCq(String v) {
        return v.replace("&#44;", ",")
                .replace("&#91;", "[")
                .replace("&#93;", "]")
                .replace("&amp;", "&");
    }

    /* 去前导空白 */
    private static String stripLeadingSpaces(String s) {
        if (s == null || s.isEmpty()) return s;
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') i++;
            else break;
        }
        return (i == 0) ? s : s.substring(i);
    }

    /* 快扫：仅剥“行首的 [CQ:reply/**] 或 [CQ:at/**]”（可重复），然后去前导空白 */
    private static String stripLeadingReplyOrAtCq(String s) {
        if (s == null || s.isEmpty()) return s;
        int i = 0, n = s.length();
        while (true) {
            // 跳过空白
            while (i < n) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') i++;
                else break;
            }
            if (i + 4 >= n || s.charAt(i) != '[' || s.charAt(i + 1) != 'C' || s.charAt(i + 2) != 'Q' || s.charAt(i + 3) != ':')
                break;

            int typeStart = i + 4, j = typeStart;
            while (j < n) {
                char c = s.charAt(j);
                if (c == ',' || c == ']') break;
                j++;
            }
            String type = s.substring(typeStart, j);
            if (!"reply".equalsIgnoreCase(type) && !"at".equalsIgnoreCase(type)) break;

            int close = s.indexOf(']', j);
            if (close < 0) break;
            i = close + 1; // 剥掉当前段
        }

        String out = s.substring(i);
        return stripLeadingSpaces(out);
    }
}

