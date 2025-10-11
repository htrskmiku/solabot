package com.arth.bot.core.config;

import com.arth.bot.adapter.controller.websocket.OneBotWsController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketControllerConfig implements WebSocketConfigurer {

    private final OneBotWsController wsController;
    private final String websocketPath;
    private final String allowedOrigins;

    public WebSocketControllerConfig(OneBotWsController wsController,
                                     @Value("${onebot.websocket.suffix}") String websocketPath,
                                     @Value("${onebot.websocket.allowed-origins}") String allowedOrigins) {
        this.wsController = wsController;
        this.websocketPath = websocketPath;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(wsController, websocketPath)
                .setAllowedOriginPatterns(allowedOrigins);
    }
}