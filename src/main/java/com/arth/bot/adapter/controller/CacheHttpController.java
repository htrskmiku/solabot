package com.arth.bot.adapter.controller;

import lombok.RequiredArgsConstructor;
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
@RequestMapping("/cache/resource")
@RequiredArgsConstructor
public class CacheHttpController {

    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * Redis 图片缓存
     * @param uuid
     * @return
     */
    @GetMapping("/imgs/{uuid}")
    public ResponseEntity<Resource> getImg(@PathVariable String uuid) {
        String key = "temp:image:png:" + uuid;
        byte[] imageBytes = redisTemplate.opsForValue().get(key);
        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        ByteArrayResource resource = new ByteArrayResource(imageBytes);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(3)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + uuid + ".png\"")
                .body(resource);
    }
}