package com.arth.bot.plugin.custom.pjsk.func;

import com.arth.bot.plugin.custom.pjsk.Pjsk;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Path;

public final class LocalResourceData {

    public static JsonNode cards;

    public static String getAssetbundleName(Pjsk.CoreBeanContext ctx, int cardId) throws IOException {
        Path p = ctx.masterDataPath().resolve("master").resolve("cards.json");
        cards = ctx.objectMapper().readTree(p.toFile());
        for (JsonNode card : cards) {
            if (cardId == card.get("id").asInt()) return card.get("assetbundleName").asText();
        }
        return null;
    }

//    private static void tryUpdateCards(Pjsk.CoreBeanContext ctx, String region) throws IOException {
//        Path root = ctx.masterDataPath().resolve(region);
//        if (!Files.exists(root)) Files.createDirectories(root);
//
//        Path versionPath = root.resolve(region).resolve("versions").resolve("current_version.json");
//        JsonNode versionInfo = ctx.objectMapper().readTree(versionPath.toFile());
//        String assetVersion = versionInfo.get("assetVersion").asText();
//
//        String latestAssetVersion = ctx.webClient().get()
//                .uri("")
//                .accept(MediaType.APPLICATION_JSON)
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
//                })
//                .map(map -> {
//                    Object value = map.get("assetVersion");
//                    return value == null ? null : value.toString();
//                })
//                .block();
//
//        if (!assetVersion.equals(latestAssetVersion)) {
//
//        }
//
//    }
}
