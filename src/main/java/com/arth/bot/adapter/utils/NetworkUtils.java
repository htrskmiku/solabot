package com.arth.bot.adapter.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

@Slf4j
public class NetworkUtils {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade"
    );

    private NetworkUtils() {
    }

    /**
     * 将请求透明转发至目标 URL，再将收到的响应透明转发回请求者
     *
     * @param webClient
     * @param targetUrl
     * @return
     */
    public static ResponseEntity<Resource> proxyRequest(
            WebClient webClient,
            String targetUrl,
            HttpServletRequest request,
            byte[] body
    ) {
        log.debug("[adapter.util] prepared to forward request...");
        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            log.error("[adapter.util] Unsupported HTTP method: {}", request.getMethod(), e);
            throw new ServerWebInputException("Unsupported HTTP method: " + request.getMethod());
        }

        URI uri = UriComponentsBuilder
                .fromUriString(targetUrl)
                .query(request.getQueryString())
                .build(true)
                .toUri();

        WebClient.RequestBodySpec requestSpec = webClient.method(method).uri(uri);

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase())) {
                Enumeration<String> values = request.getHeaders(headerName);
                List<String> valueList = Collections.list(values);
                requestSpec.header(headerName, valueList.toArray(String[]::new));
            }
        }

        WebClient.RequestHeadersSpec<?> specToSend =
                (body != null && body.length > 0)
                        ? requestSpec.bodyValue(body)
                        : requestSpec;


        log.debug("[adapter.util] forwarding request to the target url: {}", targetUrl);
        return specToSend.exchangeToMono(clientResponse -> {
            log.debug("[adapter.util] received response from {}, forwarding back...", targetUrl);
            HttpStatusCode status = clientResponse.statusCode();
            HttpHeaders headers = new HttpHeaders();
            clientResponse.headers().asHttpHeaders().forEach(headers::addAll);

            return clientResponse.bodyToMono(byte[].class)
                    .defaultIfEmpty(new byte[0])
                    .map(responseBytes -> {
                        Resource resource = new ByteArrayResource(responseBytes);
                        return ResponseEntity.status(status)
                                .headers(headers)
                                .body(resource);
                    })
                    .doOnNext(response -> log.debug("[adapter.util] forward success, status: {}", status));
        }).block();
    }


    public static byte[] readBody(HttpServletRequest request) throws IOException {
        try (InputStream inputStream = request.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    public static String getRequestInfoStr(ObjectMapper objectMapper, HttpServletRequest request) {
        Map<String, Object> requestInfo = new LinkedHashMap<>();

        // 基本信息
        requestInfo.put("method", request.getMethod());
        requestInfo.put("url", request.getRequestURL().toString());
        requestInfo.put("uri", request.getRequestURI());
        requestInfo.put("queryString", request.getQueryString());
        requestInfo.put("remoteAddr", request.getRemoteAddr());
        requestInfo.put("contentType", request.getContentType());

        // 参数
        requestInfo.put("parameters", request.getParameterMap());

        // header
        Map<String, String> simpleHeaders = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            simpleHeaders.put(name, request.getHeader(name));
        }
        requestInfo.put("headers", simpleHeaders);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestInfo);
        } catch (Exception e) {
            return "Error serializing request info: " + e.getMessage();
        }
    }
}