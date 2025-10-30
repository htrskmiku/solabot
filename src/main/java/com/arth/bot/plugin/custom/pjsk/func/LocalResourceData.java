package com.arth.bot.plugin.custom.pjsk.func;

import com.arth.bot.plugin.custom.pjsk.Pjsk;
import com.arth.bot.plugin.custom.pjsk.objects.PjskCard;
import com.arth.bot.plugin.custom.pjsk.objects.enums.CardAttributes;
import com.arth.bot.plugin.custom.pjsk.objects.enums.CardRarities;
import com.arth.bot.plugin.custom.pjsk.utils.pair.PjskCardInfo;
import com.arth.bot.plugin.resource.MemoryData;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
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


    /**
     * 通过cardId获取cards.json中的卡信息
     */
    public static boolean cached = false;
    public static PjskCardInfo getCachedCardInfo(Pjsk.CoreBeanContext ctx, int cardId) throws IOException {
        if (!cached) {
            log.info("首次使用，正在缓存卡片信息");
            Path p = ctx.masterDataPath().resolve("master").resolve("cards.json");
            JsonNode cards = ctx.objectMapper().readTree(p.toFile());
            for (JsonNode card : cards) {
                String assetbundleName = card.get("assetbundleName").asText();
                CardAttributes attribute = CardAttributes.valueOf(card
                        .get("attr")
                        .asText()
                        .toUpperCase());//sbga
                CardRarities rarities = CardRarities.valueOf(card.get("cardRarityType")
                        .asText()
                        .toUpperCase());
                MemoryData.cachedCards.put(card.get("id").asInt()-1,new PjskCardInfo(assetbundleName,attribute,rarities));
            }
            log.info("卡片信息缓存完成!");
            cached = true;
        }
//        JsonNode assetbundleNameNode = MemoryData.cachedCards.get(cardId-1);
//        String assetbundleName = assetbundleNameNode.get("assetbundleName").asText();
//        CardAttributes attribute = CardAttributes.valueOf(assetbundleNameNode
//                .get("attr")
//                .asText()
//                .toUpperCase());//sbga
//        CardRarities rarities = CardRarities.valueOf(assetbundleNameNode.get("cardRarityType").asText().toUpperCase());
//        return new PjskCardInfo(assetbundleName, attribute,rarities);
//      if (cardId == card.get("id").asInt()) return card.get("assetbundleName").asText();
        return MemoryData.cachedCards.get(cardId-1);
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
