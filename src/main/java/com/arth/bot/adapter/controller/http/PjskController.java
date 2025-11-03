package com.arth.bot.adapter.controller.http;

import com.arth.bot.adapter.controller.ApiPaths;
import com.arth.bot.adapter.utils.NetworkUtils;
import com.arth.bot.plugin.custom.pjsk.utils.Decryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
public class PjskController {

    private final Path BASE_DIR;
    private final ResourceLoader resourceLoader;
    private final WebClient webClient;

    @Autowired
    private PjskController(
            @Value("${app.local-path.pjsk-resource.dynamic.mysekai.root}") String rootPath,
            ResourceLoader resourceLoader,
            WebClient webClient
    ) {
        BASE_DIR = Path.of(rootPath).toAbsolutePath().normalize();
        this.resourceLoader = resourceLoader;
        this.webClient = webClient;
    }

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
     * 获取转发脚本的请求（暂时的实现是原封不动地转发给 python 程序处理）
     *
     * @param request
     * @return
     * @throws IOException
     */
    @GetMapping(ApiPaths.PJSK_UPLOAD_JS)
    public ResponseEntity<Resource> getUploadJs(HttpServletRequest request) throws IOException {
        byte[] body = NetworkUtils.readBody(request);
        return NetworkUtils.proxyRequest(webClient, "http://localhost:8849/upload.js", request, body);
    }

    /**
     * 处理转发数据（暂时的实现是原封不动地转发给 python 程序处理）
     *
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping(ApiPaths.PJSK_UPLOAD)
    public ResponseEntity<Resource> upload(HttpServletRequest request) throws IOException {
        byte[] body = NetworkUtils.readBody(request);
        return NetworkUtils.proxyRequest(webClient, "http://localhost:8849/upload", request, body);
    }

    @PostMapping( ApiPaths.PJSK_WEB_UPLOAD)
    public String upload(@RequestParam("file") MultipartFile file,@RequestParam("filetype") String filetype,@RequestParam("region") String region) throws IOException {
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
