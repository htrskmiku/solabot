package com.arth.bot.plugin.custom.pjsk.func;

import com.arth.bot.adapter.sender.action.ActionChainBuilder;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.database.domain.PjskBinding;
import com.arth.bot.plugin.custom.pjsk.Pjsk;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;

public final class Mysekai {

    private Mysekai() {
    }

    public static void msm(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload) {
        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        PjskBinding binding = ctx.pjskBindingMapper().selectOne(queryBinding(userId, groupId));

        if (binding == null) {
            ctx.sender().replyText(payload, "数据库中没有查询到你绑定的 pjsk 账号哦");
            return;
        }

        String pjskId = binding.getPjskId();
        String region = binding.getServerRegion();

        // Path file = Path.of(System.getProperty("user.dir") + "/dynamic/pjsk_user_data/mysekai/draw/map/" + "cn" + "_" + pjskId + ".png");
        Path file = findPjskFileTMP(ctx, pjskId);
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

        region = file.getFileName().toString().substring(0, 2);
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

    public static void bind(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload, List<String> args) {
        if (args.isEmpty()) {
            bound(ctx, payload);
            return;
        }

        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        String pjskId = args.get(0);

        // 1. 查询
        PjskBinding binding = ctx.pjskBindingMapper().selectOne(queryBinding(userId, groupId));

        // 2. 插入或更新
        if (binding == null) {
            // 不存在记录，插入
            PjskBinding newBinding = new PjskBinding();
            newBinding.setPjskId(pjskId);
            newBinding.setUserId(userId);
            newBinding.setGroupId(groupId);
            newBinding.setServerRegion("cn");
            newBinding.setCreatedAt(new Date());
            newBinding.setUpdatedAt(new Date());
            ctx.pjskBindingMapper().insert(newBinding);
            ctx.sender().sendText(payload, "bind successfully!");
        } else {
            // 存在记录，更新
            binding.setPjskId(pjskId);
            binding.setServerRegion("cn");
            binding.setUpdatedAt(new Date());
            ctx.pjskBindingMapper().updateById(binding);
            ctx.sender().sendText(payload, "pjsk binding just updated");
        }
    }

    public static void bound(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload) {
        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        PjskBinding binding = ctx.pjskBindingMapper().selectOne(queryBinding(userId, groupId));
        if (binding == null) {
            ctx.sender().replyText(payload, "you haven't bound any pjsk id yet");
        } else {
            ctx.sender().replyText(payload, "your pjsk id is " + binding.getPjskId());
        }
    }

    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****

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

    /**
     * 临时性方法，能跑就行 TMP
     *
     * @param pjskId
     * @return
     */
    @Deprecated
    private static Path findPjskFileTMP(Pjsk.CoreBeanContext ctx, String pjskId) {
        Path dir = ctx.localMapPath();
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return dir.resolve("default.png");
        }
        try {
            return Files.list(dir)
                    .filter(file -> {
                        String fileName = file.getFileName().toString();
                        return fileName.matches("[a-z]{2}_" + pjskId + "\\.png");
                    })
                    .findFirst()
                    .orElse(dir.resolve("default.png"));
        } catch (IOException e) {
            return dir.resolve("default.png");
        }
    }
}
