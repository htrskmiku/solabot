package com.arth.bot.plugins.pjsk.func;

import com.arth.bot.plugins.pjsk.Pjsk;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Data {

    public static JsonNode cards;

    public static String getAssetbundleName(Pjsk.CoreBeanContext ctx, int cardId) {
        try {
            tryUpdateCards(ctx, "jp");
        } catch (IOException e) {

        }
        return null;
    }

    private static void tryUpdateCards(Pjsk.CoreBeanContext ctx, String region) throws IOException {
        Path root = ctx.masterDataPath().resolve(region);
        if (!Files.exists(root)) Files.createDirectories(root);

        Path versionPath = root.resolve(region).resolve("versions").resolve("current_version.json");
        JsonNode versionInfo = ctx.objectMapper().readTree(versionPath.toFile());
        String assetVersion = versionInfo.get("assetVersion").asText();

        String latestAssetVersion = ctx.webClient().get()
                .uri("")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(map -> {
                    Object value = map.get("assetVersion");
                    return value == null ? null : value.toString();
                })
                .block();

    }
}
