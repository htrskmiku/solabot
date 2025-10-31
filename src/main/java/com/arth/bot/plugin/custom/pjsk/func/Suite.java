package com.arth.bot.plugin.custom.pjsk.func;

import com.arth.bot.adapter.sender.action.ActionChainBuilder;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.ExternalServiceErrorException;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.common.exception.ResourceNotFoundException;
import com.arth.bot.core.database.domain.PjskBinding;
import com.arth.bot.plugin.custom.pjsk.Pjsk;
import com.arth.bot.plugin.custom.pjsk.render.ImageRenderer;
import com.arth.bot.plugin.custom.pjsk.objects.PjskCardInfo;
import com.arth.bot.plugin.custom.pjsk.utils.pair.RegionIdPair;
import com.arth.bot.plugin.custom.pjsk.objects.PjskCard;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class Suite {

    private Suite(){}


    public static void box(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload) {
        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        PjskBinding binding = ctx.pjskBindingMapper().selectOne(queryBinding(userId, groupId));

        if (binding == null) {
            ctx.sender().replyText(payload, "数据库中没有查询到你绑定的 pjsk 账号哦");
            return;
        }

        try {
            ctx.sender().replyText(payload,"已经收到查Box请求，正在生成图片，生成时间较长，请耐心等待");
            RegionIdPair pair = getRegionAndId(ctx, payload);
            JsonNode suiteData;
            if (ctx.devel_mode()){
                suiteData = getDefaultSuite(ctx);
            }else {
                suiteData = requestSuite(ctx, pair.left(), pair.right());
            }
            JsonNode userCardsNode = suiteData.get("userCards");
            ArrayList<PjskCard> pjskCards = new ArrayList<>();
            int counts = userCardsNode.size();
            long startMs = System.currentTimeMillis();
            if (userCardsNode.isArray()) {
                for (JsonNode userCardNode : userCardsNode) {
                    PjskCardInfo info = LocalResourceData.
                            getCachedCardInfo(ctx,userCardNode.get("cardId").asInt());
                    PjskCard card = new PjskCard(userCardNode, info);
                    card.setThumbnails(new ImageRenderer.Card(ctx,card).draw());//慢死了
                    pjskCards.add(card);
                }
            }
            long stopMs = System.currentTimeMillis();
            log.info("Pjsk Box Picture rendering process: {} pictures done,Used {}ms.",counts,stopMs-startMs);
            BufferedImage boxImage = new ImageRenderer.Box(ctx,pjskCards).draw();
            String boxImgUuid = ctx.imageCacheService().cacheImage(boxImage);
            String boxImgUrl = ctx.networkEndpoint() +"/cache/resource/imgs/png/" + boxImgUuid;
            if (boxImgUuid == null) {throw new InternalServerErrorException();}

            ActionChainBuilder chainBuilder = ctx.actionChainBuilder().create()
                    .setReplay(payload.getMessageId())
                    .image(boxImgUrl);

            String json = payload.getMessageType().equals("group") ?
                    chainBuilder.toGroupJson(payload.getGroupId()) :
                    chainBuilder.toPrivateJson(payload.getUserId());

            ctx.sender().pushActionJSON(payload.getSelfId(), json);

            //ctx.sender().sendImage(payload,boxImgUrl);
            //TODO:考虑是否缓存单个已渲染的卡面 注意：卡面分为Original和SpecialTraining
        }catch (NullPointerException e){
            throw new InternalServerErrorException("Error in getting user card id");
        }catch (IOException e){
            throw new ResourceNotFoundException("Error in getting asset bundle: cards.json not found");
        }catch (ResourceNotFoundException ignored){
            //???
        }
    }

    // ***** ============= advanced helper ============= *****
    // ***** ============= advanced helper ============= *****
    // ***** ============= advanced helper ============= *****


    private static RegionIdPair getRegionAndId(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload) {
        RegionIdPair pair = new RegionIdPair();
        LambdaQueryWrapper<PjskBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PjskBinding::getUserId, payload.getUserId());
        PjskBinding binding = ctx.pjskBindingMapper().selectOne(queryWrapper);
        if (binding == null) throw new ResourceNotFoundException("user's pjsk binding data not found");
        pair.setLeft(binding.getServerRegion());
        pair.setRight(binding.getPjskId());
        return pair;
    }

    // ***** ======== FOR OFFLINE MODE ONLY ============ *****
    // ***** ======== FOR OFFLINE MODE ONLY ============ *****

    //offline_mode=true时调用
    private static JsonNode getDefaultSuite(Pjsk.CoreBeanContext ctx) {
        Path path = ctx.masterDataPath().resolve("master").resolve("default_suite.json");
        try {
            return ctx.objectMapper().readTree(path.toFile());
        }catch (IOException e) {
            throw new ResourceNotFoundException("default_suite.json not found");
        }
    }

    // ***** ======== FOR OFFLINE MODE ONLY ============ *****
    // ***** ======== FOR OFFLINE MODE ONLY ============ *****

    // ***** ============= account query  ============= *****
    // ***** ============= account query  ============= *****
    // ***** ============= account query  ============= *****

    private static LambdaQueryWrapper<PjskBinding> queryBinding(long userId, Long groupId, String serverRegion) {
        LambdaQueryWrapper<PjskBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PjskBinding::getUserId, userId).eq(PjskBinding::getServerRegion, serverRegion);
        if (groupId == null) {
            queryWrapper.isNull(PjskBinding::getGroupId);
        } else {
            queryWrapper.eq(PjskBinding::getGroupId, groupId);
        }
        return queryWrapper;
    }

    private static LambdaQueryWrapper<PjskBinding> queryBinding(long userId, Long groupId) {
        return queryBinding(userId, groupId, "cn");
    }


    // ***** ============= request helper ============= *****
    // ***** ============= request helper ============= *****
    // ***** ============= request helper ============= *****

    private static byte[] requestThumbnail(Pjsk.CoreBeanContext ctx, int cardId) {
        String url = ctx.thumbnailApi().replace("{{cardId}}", String.valueOf(cardId));
        return null;
    }

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
