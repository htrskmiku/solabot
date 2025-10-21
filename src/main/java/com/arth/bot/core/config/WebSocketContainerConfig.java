package com.arth.bot.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@ConditionalOnBean(ServletWebServerFactory.class)
public class WebSocketContainerConfig {

    /**
     * 配置 TomCat 缓冲区大小，避免 code 1009 错误：The decoded text message was too big for the output buffer and the endpoint does not support partial messages
     * TomCat 默认仅配置 8KB 缓冲区
     * @return
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024);   // 5 MB
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024); // 5 MB
        container.setMaxSessionIdleTimeout(1 * 60 * 1000L);       // 1 min
        return container;
    }
}
