package com.arth.bot.core.invoker;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 在整个链路中共享上下文与中间结果以支持链式调用
 */
@Data
public class CommandChainContext {

    private final ParsedPayloadDTO payload;
    private final Map<String, Object> bag = new HashMap<>();
    private Object state;

    public CommandChainContext(ParsedPayloadDTO payload) {
        this.payload = payload;
    }

    public <T> T getState(Class<T> t) {
        return t.cast(state);
    }
}