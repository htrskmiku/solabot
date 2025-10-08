package com.arth.bot.adapter.sender.action;

import java.util.List;
import java.util.Map;

public interface SimpleActionBuilder {

    /**
     * 构建纯文本内容的响应群聊 action 的 JSON
     * @param groupId
     * @param text
     * @return
     */
    String buildGroupSendTextAction(long groupId, String text);

    /**
     * 构建纯文本内容的响应私聊 action 的 JSON
     * @param userId
     * @param text
     * @return
     */
    String buildPrivateSendTextAction(long userId, String text);

    /**
     * 构建纯文本内容的回复群聊 action 的 JSON
     * @param groupId
     * @param messageId
     * @param text
     * @return
     */
    String buildGroupReplyTextAction(long groupId, long messageId, String text);

    /**
     * 构建纯文本内容的回复私聊 action 的 JSON
     * @param userId
     * @param messageId
     * @param text
     * @return
     */
    String buildPrivateReplyTextAction(long userId, long messageId, String text);

    /**
     * 构建图片内容的响应群聊 action 的 JSON
     * @param groupId
     * @param file
     * @return
     */
    String buildGroupSendImageAction(long groupId, String file);

    /**
     * 构建图片内容的响应私聊 action 的 JSON
     * @param userId
     * @param file
     * @return
     */
    String buildPrivateSendImageAction(long userId, String file);

    /**
     * 构建图片内容的回复群聊 action 的 JSON
     * @param groupId
     * @param messageId
     * @param file
     * @return
     */
    String buildGroupReplyImageAction(long groupId, long messageId, String file);

    /**
     * 构建图片内容的回复私聊 action 的 JSON
     * @param userId
     * @param messageId
     * @param file
     * @return
     */
    String buildPrivateReplyImageAction(long userId, long messageId, String file);

    /**
     * 构建发送视频至群聊的 action 的 JSON
     * @param groupId
     * @param file
     * @return
     */
    String buildGroupSendVideoAction(long groupId, String file);

    /**
     * 构建发送视频至私聊的 action 的 JSON
     * @param userId
     * @param file
     * @return
     */
    String buildPrivateSendVideoAction(long userId, String file);

    /**
     * 构造群聊的转发消息
     * @param groupId
     * @param nodes
     * @return
     */
    String buildGroupForwardAction(long groupId, List<Map<String, Object>> nodes);

    /**
     * 构造私聊的转发消息
     * @param userId
     * @param nodes
     * @return
     */
    String buildPrivateForwardAction(long userId, List<Map<String, Object>> nodes);
}
