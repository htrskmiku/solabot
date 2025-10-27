package com.arth.bot.plugin.custom;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.PluginRegistry;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import lombok.Setter;

public abstract class Plugin {

    @Setter
    protected PluginRegistry pluginRegistry;

    @BotCommand("index")
    public abstract void index(ParsedPayloadDTO payload);

    @BotCommand("help")
    public void help(ParsedPayloadDTO payload) {
        pluginRegistry.callPluginHelp(payload, this.getClass().getAnnotation(BotPlugin.class).value()[0]);
    }

    public abstract String getHelpText();
}
