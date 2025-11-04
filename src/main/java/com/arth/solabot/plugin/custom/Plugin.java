package com.arth.solabot.plugin.custom;

import com.arth.solabot.core.bot.dto.ParsedPayloadDTO;
import com.arth.solabot.core.bot.invoker.PluginRegistry;
import com.arth.solabot.core.bot.invoker.annotation.BotCommand;
import com.arth.solabot.core.bot.invoker.annotation.BotPlugin;
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
