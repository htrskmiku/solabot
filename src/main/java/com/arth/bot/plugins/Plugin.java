package com.arth.bot.plugins;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.PluginRegistry;
import com.arth.bot.core.invoker.annotation.BotCommand;
import lombok.Setter;

public abstract class Plugin {

    @Setter
    protected PluginRegistry pluginRegistry;

    @BotCommand("index")
    public abstract void index(ParsedPayloadDTO payload);

    @BotCommand("help")
    public abstract void help(ParsedPayloadDTO payload);

    public abstract String getHelpText();
}
