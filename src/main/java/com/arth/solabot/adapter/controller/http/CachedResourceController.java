package com.arth.solabot.adapter.controller.http;

import com.arth.solabot.adapter.controller.ApiPaths;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class CachedResourceController {

    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * Redis PNG 图片缓存
     *
     * @param uuid
     * @return
     */
    @GetMapping(ApiPaths.CACHE_IMG_PNG)
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
    @GetMapping(ApiPaths.CACHE_IMG_GIF)
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