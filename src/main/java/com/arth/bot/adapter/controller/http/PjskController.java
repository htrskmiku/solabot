package com.arth.bot.adapter.controller.http;

import com.arth.bot.adapter.controller.ApiPaths;
import com.arth.bot.adapter.utils.NetworkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
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
    private final ApiPaths apiPaths;

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
            @RequestParam("original") String original
    ) throws IOException {
        log.debug("[adapter.http] received mysekai proxy request: {}", NetworkUtils.getRequestInfoStr(objectMapper, request));
        final byte[] reqBody = NetworkUtils.readBody(request);
        ResponseEntity<Resource> upstream = NetworkUtils.proxyRequest(webClient, original, request, reqBody);
        log.debug("[adapter.http] proxied successfully status={}", upstream.getStatusCode().value());
        byte[] tmp = new byte[0];
        if (upstream.getBody() instanceof ByteArrayResource bar) {
            tmp = bar.getByteArray();
        } else if (upstream.getBody() != null) {
            try (InputStream is = upstream.getBody().getInputStream()) {
                tmp = is.readAllBytes();
            }
        }
        final byte[] upstreamBytes = tmp;
        MediaType ct = upstream.getHeaders().getContentType();
        if (ct == null) ct = MediaType.APPLICATION_OCTET_STREAM;
        final MediaType contentType = ct;
        webClient.post()
                .uri("http://localhost:8849/upload")
                .contentType(contentType)
                .header("X-Original-Url", original)
                .header("X-Upstream-Status", String.valueOf(upstream.getStatusCode().value()))
                .bodyValue(upstreamBytes)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("[adapter.http] async request failed", e))
                .subscribe(resp -> log.info("[adapter.http] python resp: {}", resp));

        return upstream;
    }


    @PostMapping(ApiPaths.PJSK_WEB_UPLOAD)
    public String upload(@RequestParam("file") MultipartFile file, @RequestParam("filetype") String filetype, @RequestParam("region") String region) throws IOException {
        byte[] body = file.getBytes();
        switch (filetype) {
            case "mysekai":
                break;
            case "suite":
                break;
        }
        return "1234";
    }//TODO:与前端对接
    /*
        前端返回格式：MultipartFile file（文件）  String filetype（文件类型，suite或mysekai）   String region（游戏区服）
        后端返回格式(JSON)：
        {
          "success" : 布尔值,
          "errormsg" : 字符串，错误信息
        }
     */


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

        Path subDir = apiPaths.getMysekaiResourceBaseDir().resolve(type).normalize();
        Path file = subDir.resolve(region + "_" + id + ".png").normalize();

        // 403: 确保最终路径仍在 BASE_DIR 下，避免目录遍历攻击
        if (!file.startsWith(apiPaths.getMysekaiResourceBaseDir())) return ResponseEntity.status(403).build();

        // 404
        if (!Files.exists(file) || !Files.isRegularFile(file)) return ResponseEntity.notFound().build();

        // 200
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
