package com.arth.bot.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebSocketContainerConfig {

    /**
     * 配置 TomCat 缓冲区大小，避免 code 1009 错误：The decoded text message was too big for the output buffer and the endpoint does not support partial messages
     * TomCat 默认仅配置 8KB 缓冲区
     * @return
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1 * 1024 * 1024);   // 1 MB
        container.setMaxBinaryMessageBufferSize(1 * 1024 * 1024); // 1 MB
        container.setMaxSessionIdleTimeout(15 * 60 * 1000L);      // 15 min
        return container;
    }
}
