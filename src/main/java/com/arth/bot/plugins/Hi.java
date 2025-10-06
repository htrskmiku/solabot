package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("plugins.hi")
@BotPlugin({"hi"})
@RequiredArgsConstructor
public class Hi {

    private final Sender sender;

    public void index(ParsedPayloadDTO payload) {
        sender.sendText(payload, "hi!");
    }
}