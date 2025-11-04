package com.arth.bot.adapter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiPaths {

    @Value("${app.client-access-network-endpoint}")
    String networkEndpoint;

    public static String DOMIN_NAME = "yly.dylancloud.uk";

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
    public static final String PJSK_SUITE_URL = "/static/pjsk/master_data/suite";
    public static final String PJSK_MYSEKAI_MAP = "/api/v1/pjsk/resource/{region}/mysekai/{id}/map";
    public static final String PJSK_MYSEKAI_OVERVIEW = "/api/v1/pjsk/resource/{region}/mysekai/{id}/overview";
    public static final String PJSK_UPLOAD_JS = "/upload.js";
    public static final String PJSK_UPLOAD = "/upload";
    public static final String PJSK_WEB_UPLOAD = "/api/upload";
    public static final String SHADOWROCKET_MODULE_DOWNLOAD_MYSEKAI_CN = "/api/v1/pjsk/module/cn/mysekai";


    public String buildMysekaiMapUrl(String region, String id) {
        return networkEndpoint + "/api/v1/pjsk/resource/" + region + "/mysekai/" + id + "/map";
    }

    public String buildMysekaiOverviewUrl(String region, String id) {
        return networkEndpoint + "/api/v1/pjsk/resource/" + region + "/mysekai/" + id + "/overview";
    }

    public String getShadowrocketModuleDownloadMysekaiCn() {
        return DOMIN_NAME + SHADOWROCKET_MODULE_DOWNLOAD_MYSEKAI_CN;
    }
}