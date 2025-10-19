package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import jakarta.annotation.Resource;

public class DefaultStrategy {

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
