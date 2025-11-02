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

    public static void msm(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload, String region) {
        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        PjskBinding binding = ctx.pjskBindingMapper().selectOne(queryBinding(userId, groupId, region));

        if (binding == null) {
            ctx.sender().replyText(payload, "数据库中没有查询到你绑定的 pjsk 账号哦");
            return;
        }

        String pjskId = binding.getPjskId();

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

    public static void msm(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload) {
        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        PjskBinding binding;
        try {
            binding = ctx.pjskBindingMapper().selectOne(queryBinding(userId, groupId));
        } catch (Exception e) {
            ctx.sender().replyText(payload, "查询到多个服务器的绑定记录，默认国服，如果要查询其他服请在 id 后空一格加上服务器地区简写，比如 jp、tw");
            binding = ctx.pjskBindingMapper().selectOne(queryBinding(userId, groupId, "cn"));
        }
    }

    public static void bind(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload, List<String> args) {
        if (args.isEmpty()) {
            bound(ctx, payload);
            return;
        }

        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        String pjskId = args.get(0);
        String region = (args.size() > 1) ? args.get(1) : "cn";

        // 1. 查询
        PjskBinding binding = ctx.pjskBindingMapper().selectOne(queryBinding(userId, groupId, region));

        // 2. 插入或更新
        if (binding == null) {
            // 不存在记录，插入
            PjskBinding newBinding = new PjskBinding();
            newBinding.setPjskId(pjskId);
            newBinding.setUserId(userId);
            newBinding.setGroupId(groupId);
            newBinding.setServerRegion(region);
            newBinding.setCreatedAt(new Date());
            newBinding.setUpdatedAt(new Date());
            ctx.pjskBindingMapper().insert(newBinding);
            if ((args.size() == 1)) {
                ctx.sender().sendText(payload, "绑定成功，默认国服，如果要绑定其他服请在 id 后空一格加上服务器地区简写，比如 jp、tw");
            } else {
                ctx.sender().sendText(payload, "绑定成功！");
            }
        } else {
            // 存在记录，更新
            binding.setPjskId(pjskId);
            binding.setServerRegion(region);
            binding.setUpdatedAt(new Date());
            ctx.pjskBindingMapper().updateById(binding);
            if ((args.size() == 1)) {
                ctx.sender().sendText(payload, "绑定已更新，默认国服，如果要绑定其他服请在 id 后空一格加上服务器地区简写，比如 jp、tw");
            } else {
                ctx.sender().sendText(payload, "绑定已更新");
            }
        }
    }

    public static void bound(Pjsk.CoreBeanContext ctx, ParsedPayloadDTO payload) {
        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        List<PjskBinding> bindings = ctx.pjskBindingMapper().selectList(queryBinding(userId, groupId));

        if (bindings == null || bindings.isEmpty()) {
            ctx.sender().replyText(payload, "You haven't bound any pjsk id yet");
        } else {
            StringBuilder reply = new StringBuilder("查询到下述绑定：\n");

            for (int i = 0; i < bindings.size(); i++) {
                PjskBinding binding = bindings.get(i);
                reply.append(i + 1)
                        .append(". Server: ")
                        .append(binding.getServerRegion())
                        .append(", ID: ")
                        .append(binding.getPjskId())
                        .append("\n");
            }

            ctx.sender().replyText(payload, reply.toString());
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
        LambdaQueryWrapper<PjskBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PjskBinding::getUserId, userId);
        if (groupId == null) {
            queryWrapper.isNull(PjskBinding::getGroupId);
        } else {
            queryWrapper.eq(PjskBinding::getGroupId, groupId);
        }
        return queryWrapper;
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
