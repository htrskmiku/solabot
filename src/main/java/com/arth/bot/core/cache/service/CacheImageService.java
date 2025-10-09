package com.arth.bot.core.cache.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CacheImageService {

    private final RedisTemplate<String, byte[]> redisTemplate;

    @Value("${server.port}")
    private String port;

    @Value("${app.client-access-url}")
    private String clientAccessUrl;

    private final String baseUrl = "http://" + clientAccessUrl + ":" + port;

    private static final Duration TTL = Duration.ofMinutes(3);

    /**
     * 缓存图片方法，返回 Redis 缓存的 UUID
     * @param img
     * @return
     * @throws IOException
     */
    public String cachePngImage(BufferedImage img) throws IOException {
        String uuid = UUID.randomUUID().toString();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] bytes = baos.toByteArray();
        redisTemplate.opsForValue().set("temp:image:png:" + uuid, bytes, TTL);
        return uuid;
    }

    /**
     * 缓存图片方法，返回 Redis 缓存的 UUIDs
     * @param imgs
     * @return
     * @throws IOException
     */
    public List<String> cachePngImage(Collection<BufferedImage> imgs) throws IOException {
        List<String> uuids = new ArrayList<>(imgs.size());
        for (BufferedImage img : imgs) {
            uuids.add(cachePngImage(img));
        }
        return uuids;
    }
}