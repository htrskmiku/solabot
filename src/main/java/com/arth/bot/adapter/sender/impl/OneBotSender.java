package com.arth.bot.adapter.sender.impl;

import com.arth.bot.adapter.fetcher.EchoWaiter;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.SimpleActionBuilder;
import com.arth.bot.adapter.session.SessionRegistry;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class OneBotSender implements Sender {

    private final SessionRegistry sessions;
    private final SimpleActionBuilder simpleActionBuilder;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final EchoWaiter echoWaiter;

    @Override
    public void sendText(ParsedPayloadDTO payload, Object text) {
        WebSocketSession session = sessions.get(payload.getSelfId());
        if (session == null || !session.isOpen() || text == null) return;

        try {
            if (text instanceof Iterable<?> it) {
                for (Object o : it) sendTextOnce(session, payload, o);
            } else {
                sendTextOnce(session, payload, text);
            }
        } catch (Exception e) {
            log.warn("[adapter] send text failed", e);
        }
    }

    @Override
    public void replyText(ParsedPayloadDTO payload, Object text) {
        WebSocketSession session = sessions.get(payload.getSelfId());
        if (session == null || !session.isOpen() || text == null) return;

        try {
            if (text instanceof Iterable<?> it) {
                for (Object o : it) responseTextOnce(session, payload, o);
            } else {
                responseTextOnce(session, payload, text);
            }
        } catch (Exception e) {
            log.warn("[adapter] send text failed", e);
        }
    }

    @Override
    public void sendImage(ParsedPayloadDTO payload, Object image) {
        WebSocketSession session = sessions.get(payload.getSelfId());
        if (session == null || !session.isOpen() || image == null) return;

        try {
            String json;
            if (image instanceof Iterable<?> it) {
                var files = toFileList(it);
                if (files.isEmpty()) return;
                if (files.size() == 1) {
                    String f = files.get(0);
                    json = payload.getGroupId() != null
                            ? simpleActionBuilder.buildGroupSendImageAction(payload.getGroupId(), f)
                            : simpleActionBuilder.buildPrivateSendImageAction(payload.getUserId(), f);
                } else {
                    json = payload.getGroupId() != null
                            ? buildMultiImageSendJson("group", Map.of("group_id", payload.getGroupId()), files)
                            : buildMultiImageSendJson("private", Map.of("user_id", payload.getUserId()), files);
                }
            } else {
                String f = String.valueOf(image);
                if (f == null || f.isBlank()) return;
                json = payload.getGroupId() != null
                        ? simpleActionBuilder.buildGroupSendImageAction(payload.getGroupId(), f)
                        : simpleActionBuilder.buildPrivateSendImageAction(payload.getUserId(), f);
            }

            log.debug("[adapter] sending image: {}", json);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.warn("[adapter] send image failed", e);
        }
    }

    @Override
    public void replyImage(ParsedPayloadDTO payload, Object image) {
        WebSocketSession session = sessions.get(payload.getSelfId());
        if (session == null || !session.isOpen() || image == null) return;

        try {
            String json;
            if (image instanceof Iterable<?> it) {
                var files = toFileList(it);
                if (files.isEmpty()) return;
                if (files.size() == 1) {
                    String f = files.get(0);
                    json = payload.getGroupId() != null
                            ? simpleActionBuilder.buildGroupReplyImageAction(payload.getGroupId(), payload.getMessageId(), f)
                            : simpleActionBuilder.buildPrivateReplyImageAction(payload.getUserId(), payload.getMessageId(), f);
                } else {
                    json = payload.getGroupId() != null
                            ? buildMultiImageResponseJson("group", Map.of("group_id", payload.getGroupId()),
                            payload.getMessageId(), files)
                            : buildMultiImageResponseJson("private", Map.of("user_id", payload.getUserId()),
                            payload.getMessageId(), files);
                }
            } else {
                String f = String.valueOf(image);
                if (f == null || f.isBlank()) return;
                json = payload.getGroupId() != null
                        ? simpleActionBuilder.buildGroupReplyImageAction(payload.getGroupId(), payload.getMessageId(), f)
                        : simpleActionBuilder.buildPrivateReplyImageAction(payload.getUserId(), payload.getMessageId(), f);
            }

            log.debug("[adapter] responding image: {}", json);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.warn("[adapter] respond image failed", e);
        }
    }

    @Override
    public void sendVideo(ParsedPayloadDTO payload, Object video) {
        WebSocketSession session = sessions.get(payload.getSelfId());
        if (session == null || !session.isOpen() || video == null) return;

        try {
            String json;
            if (video instanceof Iterable<?> it) {
                var files = toFileList(it);
                if (files.isEmpty()) return;
                if (files.size() == 1) {
                    String f = files.get(0);
                    json = payload.getGroupId() != null
                            ? simpleActionBuilder.buildGroupSendVideoAction(payload.getGroupId(), f)
                            : simpleActionBuilder.buildPrivateSendVideoAction(payload.getUserId(), f);
                } else {
                    String f = files.get(0);
                    json = payload.getGroupId() != null
                            ? simpleActionBuilder.buildGroupSendVideoAction(payload.getGroupId(), f)
                            : simpleActionBuilder.buildPrivateSendVideoAction(payload.getUserId(), f);
                    for (int i = 1; i < files.size(); i++) {
                        String remainingFile = files.get(i);
                        String remainingJson = payload.getGroupId() != null
                                ? simpleActionBuilder.buildGroupSendVideoAction(payload.getGroupId(), remainingFile)
                                : simpleActionBuilder.buildPrivateSendVideoAction(payload.getUserId(), remainingFile);
                        session.sendMessage(new TextMessage(remainingJson));
                    }
                }
            } else {
                String f = String.valueOf(video);
                if (f == null || f.isBlank()) return;
                json = payload.getGroupId() != null
                        ? simpleActionBuilder.buildGroupSendVideoAction(payload.getGroupId(), f)
                        : simpleActionBuilder.buildPrivateSendVideoAction(payload.getUserId(), f);
            }

            log.debug("[adapter] sending video: {}", json);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.warn("[adapter] send video failed", e);
        }
    }

    @Override
    public void pushActionJSON(long selfId, String json) {
        WebSocketSession session = sessions.get(selfId);
        if (session == null || !session.isOpen()) {
            log.warn("[adapter] raw json send: session missing/closed, selfId={}", selfId);
            return;
        }

        try {
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.warn("[adapter] raw json send failed, selfId={}", selfId, e);
        }
    }

    @Override
    public JsonNode request(long selfId, String action, JsonNode params, Duration timeout) {
        String echo = java.util.UUID.randomUUID().toString();
        echoWaiter.register(echo);

        ObjectNode req = objectMapper.createObjectNode();
        req.put("action", action);
        if (params != null) req.set("params", params);
        req.put("echo", echo);

        pushActionJSON(selfId, req.toString());

        try {
            String raw = echoWaiter.await(echo, timeout.toMillis()); // 等待 controller 完成的回包
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException("request '" + action + "' failed/timeout", e);
        }
    }

    @Override
    public <T> T request(long selfId, String json, JsonNode params, Duration timeout, Function<JsonNode, T> mapper) {
        JsonNode resp = request(selfId, json, params, timeout);
        return mapper.apply(resp);
    }

    private void sendTextOnce(WebSocketSession session, ParsedPayloadDTO payload, Object o) throws IOException {
        if (o == null) return;
        String json;

        if (o instanceof String s) {
            json = payload.getGroupId() != null
                    ? simpleActionBuilder.buildGroupSendTextAction(payload.getGroupId(), s)
                    : simpleActionBuilder.buildPrivateSendTextAction(payload.getUserId(), s);
        } else {
            String text = String.valueOf(o);
            json = payload.getGroupId() != null
                    ? simpleActionBuilder.buildGroupSendTextAction(payload.getGroupId(), text)
                    : simpleActionBuilder.buildPrivateSendTextAction(payload.getUserId(), text);
        }

        log.debug("[adapter] sending message: {}", json);
        session.sendMessage(new TextMessage(json));
    }

    private void responseTextOnce(WebSocketSession session, ParsedPayloadDTO payload, Object o) throws IOException {
        if (o == null) return;
        String json;

        if (o instanceof String s) {
            json = payload.getGroupId() != null
                    ? simpleActionBuilder.buildGroupReplyTextAction(payload.getGroupId(), payload.getMessageId(), s)
                    : simpleActionBuilder.buildPrivateReplyTextAction(payload.getUserId(), payload.getMessageId(), s);
        } else {
            String text = String.valueOf(o);
            json = payload.getGroupId() != null
                    ? simpleActionBuilder.buildGroupReplyTextAction(payload.getGroupId(), payload.getMessageId(), text)
                    : simpleActionBuilder.buildPrivateReplyTextAction(payload.getUserId(), payload.getMessageId(), text);
        }

        log.debug("[adapter] responding message: {}", json);
        session.sendMessage(new TextMessage(json));
    }

    // ===== helpers =====

    /** Iterable -> 去空白后的字符串列表 */
    private static List<String> toFileList(Iterable<?> it) {
        ArrayList<String> out = new ArrayList<>();
        for (Object o : it) {
            if (o == null) continue;
            String s = String.valueOf(o).trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /** 构造：多图同一条消息（不带 reply） */
    private static String buildMultiImageSendJson(String messageType, Map<String, Object> target, List<String> files) {
        Map<String, Object> root = new HashMap<>();
        root.put("action", "send_msg");
        root.put("echo", "echo-" + System.currentTimeMillis());

        Map<String, Object> params = new HashMap<>(target);
        params.put("message_type", messageType);

        List<Object> message = new ArrayList<>(files.size());
        for (String f : files) {
            message.add(imageSeg(f));
        }
        params.put("message", message);
        root.put("params", params);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("serialize multi-image send failed", e);
        }
    }

    /** 构造：多图同一条消息（带 reply） */
    private static String buildMultiImageResponseJson(String messageType, Map<String, Object> target, long messageId, List<String> files) {
        Map<String, Object> root = new HashMap<>();
        root.put("action", "send_msg");
        root.put("echo", "echo-" + System.currentTimeMillis());

        Map<String, Object> params = new HashMap<>(target);
        params.put("message_type", messageType);

        List<Object> message = new ArrayList<>(files.size() + 1);
        Map<String, Object> reply = new HashMap<>();
        reply.put("type", "reply");
        reply.put("data", Map.of("id", String.valueOf(messageId)));
        message.add(reply);

        for (String f : files) {
            message.add(imageSeg(f));
        }
        params.put("message", message);
        root.put("params", params);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("serialize multi-image response failed", e);
        }
    }

    private static Map<String, Object> imageSeg(String file) {
        Map<String, Object> seg = new HashMap<>();
        seg.put("type", "image");
        seg.put("data", Map.of("file", file));
        return seg;
    }
}