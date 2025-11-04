package com.arth.solabot.core.bot.dto;

import com.arth.solabot.core.bot.dto.message.MessageSegment;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/** 用于将传入的 OneBot v11 消息解析 POJO 的 DTO */
@Data
public class ParsedPayloadDTO {

    /* 消息类型，例如 message */
    private String postType;

    /* 消息子类型，区分群聊与私聊 */
    private String messageType;

    /* bot 账号 ID */
    private long selfId;

    /* 消息发送者账号 ID */
    private long userId;

    /* 群聊 ID，使用封装类以允许为 null */
    private Long groupId;

    /* 本条消息 ID，用于稍后可能的回复 */
    private long messageId;

    /* 引用消息 ID，用于获取本条消息所回复消息的 ID */
    private String replyToMessageId;

    /* 消息时间 */
    private long time;

    /* 原始报文中携带的纯文本段字段内容 */
    private String rawText;

    /* 消息发送者角色，例如管理员或普通成员 */
    private String senderRole;

    /* 从原始报文处理后得到的结构化消息段，例如 "@" 段、纯文本段 */
    private List<MessageSegment> segments;

    /* 命令文本 */
    private String commandText;

    /* 协议原始报文解析后的 JSON 对象 */
    private JsonNode rawRoot;

    /* 协议原始报文 */
    private String originalJsonString;
}
