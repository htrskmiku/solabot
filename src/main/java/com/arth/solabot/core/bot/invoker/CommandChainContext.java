package com.arth.solabot.core.bot.invoker;

import com.arth.solabot.core.bot.dto.ParsedPayloadDTO;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 在整个链路中共享上下文与中间结果以支持链式调用
 */
@Data
class CommandChainContext {

    private final ParsedPayloadDTO payload;
    private final Map<String, Object> bag = new HashMap<>();
    private Object state;

    CommandChainContext(ParsedPayloadDTO payload) {
        this.payload = payload;
    }

    <T> T getState(Class<T> t) {
        return t.cast(state);
    }
}