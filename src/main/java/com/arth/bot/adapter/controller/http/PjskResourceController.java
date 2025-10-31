package com.arth.bot.adapter.controller.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping("${app.api-path.plugin.pjsk.root}")
public class PjskResourceController {

    private final Path BASE_DIR;

    @Autowired
    private PjskResourceController(@Value("${app.local-path.pjsk-resource.dynamic.mysekai.root}") String rootPath) {
        BASE_DIR = Path.of(rootPath).toAbsolutePath().normalize();
    }

    @GetMapping("${app.api-path.plugin.pjsk.mysekai.map}")
    public ResponseEntity<Resource> getMysekaiMap(@PathVariable String region, @PathVariable String id) throws IOException {
        return buildMysekaiResourceUrlSafely("map", region, id);
    }

    @GetMapping("${app.api-path.plugin.pjsk.mysekai.overview}")
    public ResponseEntity<Resource> getMysekaiOverview(@PathVariable String region, @PathVariable String id) throws IOException {
        return buildMysekaiResourceUrlSafely("overview", region, id);
    }

//    @GetMapping("")  // 预计是提供模块配置的下载

    /**
     * 安全的响应构造方法，避免路径遍历攻击
     *
     * @param type
     * @param region
     * @param id
     * @return
     * @throws IOException
     */
    private ResponseEntity<Resource> buildMysekaiResourceUrlSafely(String type, String region, String id) throws IOException {
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
