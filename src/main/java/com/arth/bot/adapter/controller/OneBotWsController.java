package com.arth.bot.adapter.controller;

import com.arth.bot.adapter.fetcher.EchoWaiter;
import com.arth.bot.adapter.parser.PayloadParser;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.session.SessionRegistry;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.CommandInvoker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OneBotWsController extends TextWebSocketHandler {

    private final SessionRegistry sessionRegistry;
    private final ExecutorService executorService;
    private final CommandInvoker commandInvoker;
    private final Sender sender;
    private final PayloadParser payloadParser;
    private final ObjectMapper objectMapper;
    private final EchoWaiter echoWaiter;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[adapter] ws connected: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String rawPayload = message.getPayload();
            JsonNode root = objectMapper.readTree(rawPayload);

            /* 分流消息（拦截 echo） */
            final JsonNode echoNode = root.get("echo");
            if (echoNode != null && !echoNode.isNull() && root.get("post_type") == null) {
                final String echo = echoNode.asText();
                if (echoWaiter.complete(echo, rawPayload)) return;
            }

            /* 注册 session，绑定至 self_id */
            if (session.getAttributes().get("self_id") == null) {
                final long selfId = root.path("self_id").asLong(0L);
                if (selfId != 0L) {
                    sessionRegistry.put(selfId, session);
                    session.getAttributes().put("self_id", selfId);
                }
            }

            ParsedPayloadDTO dto = payloadParser.parseRawToDTO(root, rawPayload);

            if ("message".equals(dto.getPostType()) || "message_sent".equals(dto.getPostType())) {
                log.debug("[adapter] received message: {}", rawPayload);

                /* 多线程异步解析命令 */
                executorService.execute(() -> {
                    try {
                        commandInvoker.parseAndInvoke(dto);
                    } catch (Exception e) {
                        log.error("[adapter] async handle error", e);
                    }
                });
            }

        } catch (Exception e) {
            log.error("[adapter] ws pipeline error", e);
        }
    }

//    private void writeResultToSession(WebSocketSession session, Object res) {
//        if (res == null || !session.isOpen()) return;
//        synchronized (session) {
//            try {
//                if (res instanceof Iterable<?> it) {
//                    for (Object o : it) {
//                        if (o == null) continue;
//                        session.sendMessage(new TextMessage(String.valueOf(o)));
//                    }
//                } else {
//                    session.sendMessage(new TextMessage(String.valueOf(res)));
//                }
//            } catch (Exception e) {
//                log.warn("[adapter] send failed", e);
//            }
//        }
//    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[adapter] transport error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object sid = session.getAttributes().get("self_id");
        if (sid instanceof Long selfId) sessionRegistry.remove(selfId);
        log.info("[adapter] ws closed: {}", session.getId());
    }
}
