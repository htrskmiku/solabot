package com.arth.bot.adapter.sender.action;

import java.util.function.Consumer;

/**
 * 链式调用构造转发消息记录
 */
public interface ForwardChainBuilder {

    ForwardChainBuilder create();

    ForwardChainBuilder addRefNode(long messageId);

    ForwardChainBuilder addCustomNode(long userId, String nickname, Consumer<ForwardChainBuilder.NodeContent> builder);

    String toGroupJson(long groupId);

    String toPrivateJson(long userId);

    interface NodeContent {
        NodeContent text(String text);
        NodeContent image(String file);
        NodeContent at(long qq);
    }
}