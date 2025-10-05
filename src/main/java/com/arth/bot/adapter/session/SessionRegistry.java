package com.arth.bot.adapter.session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册 WebSocket session，将 self_id 绑定至 session
 */
@Component
public class SessionRegistry {

    private final Map<Long, WebSocketSession> map = new ConcurrentHashMap<>();

    public void put(long selfId, WebSocketSession session) {
        map.put(selfId, session);
    }

    public void remove(long selfId) {
        map.remove(selfId);
    }

    public WebSocketSession get(long selfId) {
        return map.get(selfId);
    }

    public void putIfAbsent(long selfId, WebSocketSession session) {
        map.putIfAbsent(selfId, session);
    }
}