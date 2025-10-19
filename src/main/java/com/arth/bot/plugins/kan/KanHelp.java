package com.arth.bot.plugins.kan;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.PluginRegistry;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.plugins.Plugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@BotPlugin({"看"})
@RequiredArgsConstructor
public class KanHelp extends Plugin {

    @Getter
    public final String helpText = """
                        看看模块暂时只支持看miku哦
                        使用方法是：/看miku
                        注意与其他模块与命令不同，这里不需要空格""";

    @BotCommand({"index", "help"})
    public void index(ParsedPayloadDTO payload) {
        help(payload);
    }

    @Override
    public void help(ParsedPayloadDTO payload) {
        pluginRegistry.callPluginHelp(payload, this.getClass().getAnnotation(BotPlugin.class).value()[0]);
    }
}