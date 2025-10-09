package com.arth.bot.adapter.sender;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.function.Function;

public interface Sender {

    /**
     * 控制 bot 发送文本字符串（非回复），接收 String 或 List<String>
     * 传入 List<String> 时，遍历发送
     * @param payload
     * @param text
     */
    void sendText(ParsedPayloadDTO payload, Object text);

    /**
     * 控制 bot 发送回复文本字符串，接收 String 或 List<String>
     * 传入 List<String> 时，遍历发送
     * @param payload
     * @param text
     */
    void replyText(ParsedPayloadDTO payload, Object text);

    /**
     * 控制 bot 发送一张图片（非回复），接收表示文件地址的 String 或 List<String>
     * 支持在同一消息中发送多张图片，传入 List<String> 即可
     * @param payload
     * @param image
     */
    void sendImage(ParsedPayloadDTO payload, Object image);

    /**
     * 控制 bot 发送一张回复图片，接收表示文件地址的 String 或 List<String>
     * 支持在同一消息中发送多张图片，传入 List<String> 即可
     * @param payload
     * @param image
     */
    void replyImage(ParsedPayloadDTO payload, Object image);

    /**
     * 控制 bot 发送视频，接收 String
     * @param payload
     * @param video
     */
    void sendVideo(ParsedPayloadDTO payload, Object video);

    /**
     * 直接向 bot 推送原始的 action JSON，不构造 action
     * forward 需要该接口
     * @param selfId
     * @param json
     */
    void pushActionJSON(long selfId, String json);

    /**
     * 向 bot 发起请求，例如请求引用消息的内容
     * 这依赖于 EchoWaiter 以实现 “请求-响应”
     * @param selfId
     * @param action
     * @param params
     * @param timeout
     * @return
     */
    JsonNode request(long selfId, String action, JsonNode params, Duration timeout);

    <T> T request(long selfId, String json, JsonNode params, Duration timeout, Function<JsonNode, T> mapper);
}
