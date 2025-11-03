package com.arth.bot.adapter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ApiPaths {

    /*
      CacheService 相关路径
    */
    public static final String CACHE_IMG_PNG = "/api/v1/cache/imgs/png/{uuid}";
    public static final String CACHE_IMG_GIF = "/api/v1/cache/imgs/gif/{uuid}";
    public static final String CACHE_IMG = "/api/v1/cache/imgs/{type}/{uuid}";

    public String buildPngUrl(String uuid) {
        return networkEndpoint + "/api/v1/cache/imgs/png/" + uuid;
    }

    public String buildGifUrl(String uuid) {
        return networkEndpoint + "/api/v1/cache/imgs/gif/" + uuid;
    }

    public String buildImgUrl(String type, String uuid) {
        return networkEndpoint + "/api/v1/cache/imgs/" + type + "/" + uuid;
    }


    /*
      PJSK 相关路径
     */
    public static final String PJSK_MYSEKAI_MAP = "/api/v1/pjsk/resource/{region}/mysekai/{id}/map";
    public static final String PJSK_MYSEKAI_OVERVIEW = "/api/v1/pjsk/resource/{region}/mysekai/{id}/overview";
    public static final String PJSK_UPLOAD_JS = "/upload.js";
    public static final String PJSK_UPLOAD = "/upload";
    public static final String PJSK_WEB_UPLOAD = "/api/upload";
    public static final String SHADOWROCKET_MODULE_DOWNLOAD_MYSEKAI_CN = "/api/v1/pjsk/module/cn/mysekai";
    public static final String MYSEKAI_UPLOAD_PROXY = "/api/v1/pjsk/upload/mysekai";

    public String buildMysekaiMapUrl(String region, String id) {
        return networkEndpoint + "/api/v1/pjsk/resource/" + region + "/mysekai/" + id + "/map";
    }

    public String buildMysekaiOverviewUrl(String region, String id) {
        return networkEndpoint + "/api/v1/pjsk/resource/" + region + "/mysekai/" + id + "/overview";
    }

    public String getShadowrocketModuleDownloadMysekaiCn() {
        return DOMIN_NAME + SHADOWROCKET_MODULE_DOWNLOAD_MYSEKAI_CN;
    }


    /*
      local path
     */
    public Path getMysekaiResourceBaseDir() {
        Path result = mysekaiResrouceBaseDir;
        if (result == null) {
            synchronized (this) {
                result = mysekaiResrouceBaseDir;
                if (result == null) {
                    mysekaiResrouceBaseDir = result = Path.of(rootPath).toAbsolutePath().normalize();
                }
            }
        }
        return result;
    }


    // **============--  Autowired Filed   --============**
    // **============--  Autowired Filed   --============**
    // **============--  Autowired Filed   --============**


    @Value("${app.client-access-network-endpoint}") String networkEndpoint;

    @Value("${app.local-path.pjsk-resource.dynamic.mysekai.root}") String rootPath;
    public volatile Path mysekaiResrouceBaseDir;

    public static String DOMIN_NAME = "yly.dylancloud.uk";
}