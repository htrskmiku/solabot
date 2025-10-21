package com.arth.bot.plugins.kan;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.cache.service.GalleryCacheService;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.plugins.Plugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@BotPlugin({"看"})
@RequiredArgsConstructor
public class Kan extends Plugin {

    private final Sender sender;
    private final GalleryCacheService galleryCacheService;

    @Getter
    public final String helpText = """
                        看看模块现支持看：
                          - 所有烤 oc，例如 saki、mnr
                          - 所有 v，例如 miku、kaito
                          - 看猪
                          - 看鸟
                          - 看夜鹭
                          - 看西巴
                          - 看kiwi
                        使用方法示例是：/看miku
                        
                        二级命令（需要空格）：
                          - fresh: 强制刷新缓存
                        
                        图源 API 来自 luna茶！""";

    @BotCommand("index")
    public void index(ParsedPayloadDTO payload) {
        sender.replyText(payload, "看命令不需要接空格哦，正确使用方法比如 “/看saki”");
    }

    @BotCommand("help")
    public void help(ParsedPayloadDTO payload) {
        pluginRegistry.callPluginHelp(payload, "看");
    }

    @BotCommand("fresh")
    public void freshCache(ParsedPayloadDTO payload) {
        galleryCacheService.forceRefreshCache();
        sender.replyText(payload, "已重置缓存…");
    }
}