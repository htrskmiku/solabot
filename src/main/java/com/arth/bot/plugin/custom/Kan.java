package com.arth.bot.plugin.custom;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.cache.service.GalleryCacheService;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.plugin.resource.MemoryData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@BotPlugin(value = {"看"}, glued = true)
@RequiredArgsConstructor
public class Kan extends Plugin {

    private final Sender sender;
    private final GalleryCacheService galleryCacheService;

    @Getter
    public final String helpText = """
                        看看你的模块现支持看：
                          - 所有烤 oc，例如 saki、mnr
                          - 所有 v，例如 miku、kaito，包括 teto
                          - 看猪，鸟，夜鹭，西巴，kiwi，企鹅

                        这些是紧凑命令，无需空格，使用方法示例是：/看miku
                        也可以按 pid 查看。例如：/看912
                        "/看看" 则会在全角色内随机
                        
                        非紧凑命令（需要空格）：
                          - fresh: 强制刷新缓存
                        
                        图源 API 来自 luna茶！""";

    @BotCommand("index")
    public void index(ParsedPayloadDTO payload, List<String> args) {
        for (String arg : args) {
            // 空字符
            if (arg == null || arg.isEmpty()) continue;

            // 全局随机
            if (arg.equals("看") || arg.equals("看你的")) {
                sender.sendImage(payload, galleryCacheService.getRandomPicUrlGlobal());
                continue;
            }

            // 如果输入是纯数字，视为 pid
            if (arg.matches("\\d+")) {
                sender.sendImage(payload, galleryCacheService.getPicUrlByPid(arg));
                continue;
            }

            // 否则解释为别名
            String alias = MemoryData.alias.get(arg);
            if (alias != null) {
                sender.sendImage(payload, galleryCacheService.getRandomPicUrl(alias));
                continue;
            }
        }
    }

    @Override
    public void index(ParsedPayloadDTO payload) {

    }

    @BotCommand("help")
    public void help(ParsedPayloadDTO payload) {
        super.help(payload);
    }

    @BotCommand("fresh")
    public void freshCache(ParsedPayloadDTO payload) {
        galleryCacheService.forceRefreshCache();
        sender.replyText(payload, "已重置缓存…");
    }
}