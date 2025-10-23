package com.arth.bot.adapter.controller.http;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("${app.api-path.cache.root}")
@RequiredArgsConstructor
public class CachedResourceController {

    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * Redis PNG 图片缓存
     *
     * @param uuid
     * @return
     */
    @GetMapping("${app.api-path.cache.img.png}" + "{uuid}")
    public ResponseEntity<Resource> getPng(@PathVariable String uuid) {
        // 400: 校验 uuid 格式
        if (!uuid.matches("[a-zA-Z0-9_-]+")) return ResponseEntity.badRequest().build();

        String key = "temp:image:png:" + uuid;
        byte[] imageBytes = redisTemplate.opsForValue().get(key);

        // 404
        if (imageBytes == null || imageBytes.length == 0) return ResponseEntity.notFound().build();

        // 200
        ByteArrayResource resource = new ByteArrayResource(imageBytes);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(3)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + uuid + ".png\"")
                .body(resource);
    }

    /**
     * Redis GIF 图片缓存
     *
     * @param uuid
     * @return
     */
    @GetMapping("${app.api-path.cache.img.gif}" + "{uuid}")
    public ResponseEntity<Resource> getGif(@PathVariable String uuid) {
        // 400: 校验 uuid 格式
        if (!uuid.matches("[a-zA-Z0-9_-]+")) return ResponseEntity.badRequest().build();

        String key = "temp:image:gif:" + uuid;
        byte[] imageBytes = redisTemplate.opsForValue().get(key);

        // 404
        if (imageBytes == null || imageBytes.length == 0) return ResponseEntity.notFound().build();

        // 200
        ByteArrayResource resource = new ByteArrayResource(imageBytes);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(3)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + uuid + ".gif\"")
                .body(resource);
    }
}