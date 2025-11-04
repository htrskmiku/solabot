package com.arth.bot.plugin.custom.pjsk.func;

import com.arth.bot.adapter.sender.action.ActionChainBuilder;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.common.exception.ResourceNotFoundException;
import com.arth.bot.core.database.domain.PjskBinding;
import com.arth.bot.plugin.custom.pjsk.Pjsk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public final class Mysekai {

    private Mysekai() {
    }

    public static void msm(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload, String region) {
        PjskBinding binding = General.queryBinding(ctx, payload.getUserId());
        if (binding == null) {
            ctx.sender().replyText(payload, "数据库中没有查询到你绑定的 pjsk 账号哦");
        } else {
            String pjskId = General.queryPjskIdByRegion(ctx, binding, region);
            if (pjskId == null) {
                ctx.sender().replyText(payload, "该账号没有绑定 " + region + " 服务器的游戏账号");
            } else {
                msmHelper(ctx, payload, pjskId, region);
            }
        }
    }

    public static void msm(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload) {
        PjskBinding binding = General.queryBinding(ctx, payload.getUserId());
        if (binding == null) {
            ctx.sender().replyText(payload, "数据库中没有查询到你绑定的 pjsk 账号哦");
        } else {
            General.IdRegionPair pair = General.queryDefaultPjskId(ctx, binding);
            msmHelper(ctx, payload, pair.pjskId(), pair.region());
        }
    }

    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****

    public static void msmHelper(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload, String pjskId, String region) {
        Path file = getFilePath(ctx, region, pjskId);
        String updatedTime = null;

        if (!Files.exists(file)) {
            ctx.sender().replyText(payload, "服务器上没有找到你的 MySekai 数据，可能是抓包未成功，小概率服务器解析失败，需要根据日志分析");
            return;
        } else {
            try {
                FileTime timestamp = Files.readAttributes(file, BasicFileAttributes.class).lastModifiedTime();
                updatedTime = ctx.dateTimeFormatter().format(timestamp.toInstant());
            } catch (IOException e) {
                ctx.sender().replyText(payload, "MySekai 数据存在，但获取更新日期失败: 抛出了 IOException");
                throw new InternalServerErrorException("IOException: " + e.getCause().getMessage(), "MySekai 数据存在，但获取更新日期失败: IOException");
            }
        }

        String overviewImgUrl = ctx.apiPaths().buildMysekaiOverviewUrl(region, pjskId);
        String mapImgUrl = ctx.apiPaths().buildMysekaiMapUrl(region, pjskId);
        ActionChainBuilder builder = ctx.actionChainBuilder().create().setReplay(payload.getMessageId())
                .text("MySekai 数据更新于" + updatedTime)
                .image(overviewImgUrl)
                .image(mapImgUrl);

        String json = payload.getMessageType().equals("group") ?
                builder.toGroupJson(payload.getGroupId()) :
                builder.toPrivateJson(payload.getUserId());

        ctx.sender().pushActionJSON(payload.getSelfId(), json);
    }

    private static Path getFilePath(Pjsk.CoreBeanContext ctx, String region, String pjskId) {
        Path dir = ctx.localMapPath();
        if (!Files.exists(dir) || !Files.isDirectory(dir)) throw new ResourceNotFoundException("path does not exist");
        Path filePath = dir.resolve(region + "_" + pjskId + ".png");
        if (!Files.exists(filePath)) throw new ResourceNotFoundException("File not found: " + filePath.getFileName());
        return filePath;
    }

    // ***** ============= uploaded file processor ============= *****
    // ***** ============= uploaded file processor ============= *****
    // ***** ============= uploaded file processor ============= *****
    private static void processMysekaiFile(){

    }
}
