package com.arth.solabot.plugin.system;

import com.arth.solabot.adapter.sender.Sender;
import com.arth.solabot.core.bot.dto.ParsedPayloadDTO;
import jakarta.annotation.Resource;

public final class DefaultStrategy {

    @Resource
    private Sender sender;

    /**
     * 若未匹配到命令，执行该默认策略
     * 例如，可以接入 LLM？
     */
    public void defaultHandle(ParsedPayloadDTO payload) {
        // sender.sendText(payload, payload.getRawText());
    }


}
