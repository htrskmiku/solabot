package com.arth.bot.plugins.pjsk.func;

import com.arth.bot.core.common.exception.ExternalServiceErrorException;
import com.arth.bot.plugins.pjsk.Pjsk;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public final class Suite {

    private Suite() {
    }

    // ***** ============= advanced helper ============= *****
    // ***** ============= advanced helper ============= *****
    // ***** ============= advanced helper ============= *****




    // ***** ============= basic request function ============= *****
    // ***** ============= basic request function ============= *****
    // ***** ============= basic request function ============= *****

    private static JsonNode requestSuite(Pjsk.CoreBeanContext ctx, String region, String id) {
        String url = ctx.suiteApi().replace("{region}", region).replace("{id}", id);
        return requestUrl(ctx, url);
    }

    private static JsonNode requestSuite(Pjsk.CoreBeanContext ctx, String region, String id, String key) {
        String url = ctx.suiteApi().replace("{region}", region).replace("{id}", id) + "?key=" + key;
        return requestUrl(ctx, url);
    }

    private static JsonNode requestSuite(Pjsk.CoreBeanContext ctx, String region, String id, List<String> keys) {
        String keyParam = String.join(",", keys);
        String url = ctx.suiteApi().replace("{region}", region).replace("{id}", id) + "?key=" + keyParam;
        return requestUrl(ctx, url);
    }

    private static JsonNode requestMysekai(Pjsk.CoreBeanContext ctx, String region, String id) {
        String url = ctx.mysekaiApi().replace("{region}", region).replace("{id}", id);
        return requestUrl(ctx, url);
    }

    private static JsonNode requestMysekai(Pjsk.CoreBeanContext ctx, String region, String id, String key) {
        String url = ctx.mysekaiApi().replace("{region}", region).replace("{id}", id) + "?key=" + key;
        return requestUrl(ctx, url);
    }

    private static JsonNode requestMysekai(Pjsk.CoreBeanContext ctx, String region, String id, List<String> keys) {
        String keyParam = String.join(",", keys);
        String url = ctx.mysekaiApi().replace("{region}", region).replace("{id}", id) + "?key=" + keyParam;
        return requestUrl(ctx, url);
    }

    /**
     * 通用请求方法，基于 WebClient。
     * 会在当前线程阻塞直到获取响应（适合 Spring MVC 环境）。
     */
    private static JsonNode requestUrl(Pjsk.CoreBeanContext ctx, String url) {
        WebClient webClient = ctx.webClient();
        try {
            String responseBody = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new IOException(
                                            "Request failed with status code: " + response.statusCode() + "\n" + body)))
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (responseBody == null) {
                throw new IOException("Empty response body for URL: " + url);
            }
            return ctx.objectMapper().readTree(responseBody);
        } catch (Exception e) {
            throw new ExternalServiceErrorException("Failed to request URL: " + url, e.getMessage());
        }
    }
}
