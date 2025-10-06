package com.arth.bot.core.config;

import io.lettuce.core.RedisURI;
import io.lettuce.core.protocol.ProtocolVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

//@Slf4j
//@Configuration
//public class RedisLettuceConfig {
//
//    /**
//     * 强制 Lettuce 回退到 RESP2 协议
//     * Lettuce 6.6 默认通过 RESP3 协议与 HELLO 命令握手，没有密码且未认证时报错 NOAUTH HELLO
//     */
//    @Bean
//    @Primary
//    public LettuceConnectionFactory redisConnectionFactory() {
//
//        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
//        redisConfig.setHostName("localhost");
//        redisConfig.setPort(6379);
//
//        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
//                .clientOptions(io.lettuce.core.ClientOptions.builder()
//                        .protocolVersion(ProtocolVersion.RESP3)
//                        .build())
//                .build();
//
//        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
//        factory.setShareNativeConnection(false);
//        factory.afterPropertiesSet();
//        return factory;
//    }
//}
