package com.arth.bot.adapter.controller.http;

import com.arth.bot.adapter.controller.ApiPaths;
import com.arth.bot.adapter.utils.NetworkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PjskController {

    private final ResourceLoader resourceLoader;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Mysekai 透视 map 请求
     *
     * @param region
     * @param id
     * @return
     * @throws IOException
     */
    @GetMapping(ApiPaths.PJSK_MYSEKAI_MAP)
    public ResponseEntity<Resource> getMysekaiMap(@PathVariable String region, @PathVariable String id) throws IOException {
        return buildMysekaiResourceUrlSafely("map", region, id);
    }

    /**
     * Mysekai 透视 overview 请求
     *
     * @param region
     * @param id
     * @return
     * @throws IOException
     */
    @GetMapping(ApiPaths.PJSK_MYSEKAI_OVERVIEW)
    public ResponseEntity<Resource> getMysekaiOverview(@PathVariable String region, @PathVariable String id) throws IOException {
        return buildMysekaiResourceUrlSafely("overview", region, id);
    }

    /**
     * Shadowrocket 模块下载请求：国服 Mysekai
     *
     * @return
     * @throws IOException
     */
    @GetMapping(ApiPaths.SHADOWROCKET_MODULE_DOWNLOAD_MYSEKAI_CN)
    public ResponseEntity<Resource> getShadowrocketModuleForCnMysekai() {
        try {
            Resource resource = resourceLoader.getResource("classpath:static/pjsk/proxy_software_module/shadowrocket/cn_mysekai.txt");

            if (!resource.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/plain; charset=utf-8"))
                    .header("Content-Disposition", "inline; filename=\"cn_mysekai.txt\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

//    /**
//     * 获取转发脚本的请求
//     *
//     * @param request
//     * @return
//     * @throws IOException
//     */
//    @GetMapping(ApiPaths.PJSK_UPLOAD_JS)
//    public ResponseEntity<Resource> getUploadJs(HttpServletRequest request) throws IOException {
//        try {
//            Resource resource = resourceLoader.getResource("classpath:static/pjsk/proxy_software_module/script.js");
//
//            if (!resource.exists()) return ResponseEntity.notFound().build();
//
//            return ResponseEntity.ok()
//                    .contentType(MediaType.parseMediaType("text/plain; charset=utf-8"))
//                    .header("Content-Disposition", "inline; filename=\"script.js\"")
//                    .body(resource);
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().build();
//        }
//    }

    /**
     * 模块重定向至此处，后端负责向游戏服务器请求数据
     *
     * @param request
     * @param original
     * @return
     * @throws IOException
     */
    @RequestMapping(path = ApiPaths.MYSEKAI_UPLOAD_PROXY, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Resource> proxyUpload(
            HttpServletRequest request,
            @RequestParam("original") String original,
            @RequestParam("user") String user,
            @RequestParam("force") String force
    ) throws IOException {
        log.debug("[adapter.http] received mysekai proxy request: {}", NetworkUtils.getRequestInfoStr(objectMapper, request));
        byte[] body = NetworkUtils.readBody(request);
        String targetUrl = original.replace("{user}", user).replace("{force}", force);
        ResponseEntity<Resource> response = NetworkUtils.proxyRequest(webClient, targetUrl, request, body);
        log.debug("[adapter.http] proxied successfully");

        CompletableFuture.runAsync(() -> {
            try {
                webClient.post()
                        .uri("http://localhost:8849/upload")
                        .contentType(response.getHeaders().getContentType())
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } catch (Exception e) {
                log.error("[adapter.http] async request failed: " + e.getMessage());
            }
        });

        return response;
    }


    // **============  helper  ============**
    // **============  helper  ============**
    // **============  helper  ============**


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

        Path subDir = ApiPaths.mysekaiResrouceBaseDir.resolve(type).normalize();
        Path file = subDir.resolve(region + "_" + id + ".png").normalize();

        // 403: 确保最终路径仍在 BASE_DIR 下，避免目录遍历攻击
        if (!file.startsWith(ApiPaths.mysekaiResrouceBaseDir)) return ResponseEntity.status(403).build();

        // 404
        if (!Files.exists(file) || !Files.isRegularFile(file)) return ResponseEntity.notFound().build();

        // 200
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
