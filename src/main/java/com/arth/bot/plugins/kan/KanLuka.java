package com.arth.bot.plugins.kan;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.cache.service.GalleryCacheService;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.plugins.Plugin;
import lombok.RequiredArgsConstructor;

@BotPlugin({"看luka", "看巡音流歌", "看流歌", "看鱼"})
@RequiredArgsConstructor
public class KanLuka extends Plugin {

    private final Sender sender;
    private final GalleryCacheService galleryCacheService;

    @BotCommand("index")
    public void index(ParsedPayloadDTO payload) {
        sender.sendImage(payload, galleryCacheService.getRandomPicUrl("luka"));
    }

    @BotCommand("help")
    public void help(ParsedPayloadDTO payload) {
        pluginRegistry.callPluginHelp(payload, "看");
    }

    @Override
    public String getHelpText() {
        return pluginRegistry.getPluginHelpText("看");
    }
}