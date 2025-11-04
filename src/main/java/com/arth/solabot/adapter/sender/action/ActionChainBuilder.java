package com.arth.solabot.adapter.sender.action;

import com.arth.solabot.adapter.sender.action.impl.OneBotActionChainBuilder;

import java.util.Map;

public interface ActionChainBuilder {

    OneBotActionChainBuilder create();

    OneBotActionChainBuilder setReplay(long messageId);

    OneBotActionChainBuilder text(String text);

    OneBotActionChainBuilder image(String file);

    OneBotActionChainBuilder at(long userId);

    OneBotActionChainBuilder custom(String type, Map<String, Object> data);

    OneBotActionChainBuilder clear();

    String toGroupJson(long groupId);

    String toPrivateJson(long userId);

}
