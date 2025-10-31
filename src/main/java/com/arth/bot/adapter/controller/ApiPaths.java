package com.arth.bot.adapter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiPaths {

    @Value("${app.client-access-network-endpoint}")
    String networkEndpoint;

    // CacheService 相关路径
    public static final String CACHE_IMG_PNG = "/cache/resource/imgs/png/{uuid}";
    public static final String CACHE_IMG_GIF = "/cache/resource/imgs/gif/{uuid}";
    public static final String CACHE_IMG = "/cache/resource/{type}/gif/{uuid}";

    public String buildPngUrl(String uuid) {
        return networkEndpoint + "/cache/resource/imgs/png/" + uuid;
    }

    public String buildGifUrl(String uuid) {
        return networkEndpoint + "/cache/resource/imgs/gif/" + uuid;
    }

    public String buildImgUrl(String type, String uuid) {
        return networkEndpoint + "/cache/resource/" + type + "/gif/" + uuid;
    }


    // PJSK 相关路径
    public static final String PJSK_MYSEKAI_MAP = "/pjsk/resource/{region}/mysekai/{id}/map";
    public static final String PJSK_MYSEKAI_OVERVIEW = "/pjsk/resource/{region}/mysekai/{id}/overview";

    public String buildMysekaiMapUrl(String region, String id) {
        return networkEndpoint + "/pjsk/resource/" + region + "/mysekai/" + id + "/map";
    }

    public String buildMysekaiOverviewUrl(String region, String id) {
        return networkEndpoint + "/pjsk/resource/" + region + "/mysekai/" + id + "/overview";
    }
}