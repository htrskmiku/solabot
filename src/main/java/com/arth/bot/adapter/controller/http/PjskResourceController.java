package com.arth.bot.adapter.controller.http;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/pjsk/resource")
public class PjskResourceController {

    private static final Path BASE_DIR = Path.of("dynamic/pjsk_user_data/mysekai/draw").toAbsolutePath().normalize();

    @GetMapping("/{region}/mysekai/{id}/map")
    public ResponseEntity<Resource> getMysekaiMap(@PathVariable String region, @PathVariable String id) throws IOException {
        return buildPjskResourceUrlSafely("map", region, id);
    }

    @GetMapping("/{region}/mysekai/{id}/overview")
    public ResponseEntity<Resource> getMysekaiOverview(@PathVariable String region, @PathVariable String id) throws IOException {
        return buildPjskResourceUrlSafely("overview", region, id);
    }

    /**
     * 安全的方法
     *
     * @param type
     * @param region
     * @param id
     * @return
     * @throws IOException
     */
    private ResponseEntity<Resource> buildPjskResourceUrlSafely(String type, String region, String id) throws IOException {
        // 400: 校验 region 与 id 格式
        if (!region.matches("[a-zA-Z0-9_-]+") || !id.matches("[a-zA-Z0-9_-]+")) {
            return ResponseEntity.badRequest().build();
        }

        Path subDir = BASE_DIR.resolve(type).normalize();
        Path file = subDir.resolve(region + "_" + id + ".png").normalize();

        // 403: 确保最终路径仍在 BASE_DIR 下，避免目录遍历攻击
        if (!file.startsWith(BASE_DIR)) return ResponseEntity.status(403).build();

        // 404
        if (!Files.exists(file) || !Files.isRegularFile(file)) return ResponseEntity.notFound().build();

        // 200
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
