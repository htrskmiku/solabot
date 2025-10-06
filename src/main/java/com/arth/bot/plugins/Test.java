package com.arth.bot.plugins;


import com.arth.bot.adapter.fetcher.ReplyFetcher;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ForwardChainBuilder;
import com.arth.bot.core.authorization.annotation.DirectAuthInterceptor;
import com.arth.bot.core.authorization.model.AuthMode;
import com.arth.bot.core.authorization.model.AuthScope;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.dto.ReplayedMessagePayloadDTO;
import com.arth.bot.core.common.dto.replay.ImageRef;
import com.arth.bot.core.common.dto.replay.MediaSourceType;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@BotPlugin({"test"})
@Component("plugins.test")
@RequiredArgsConstructor
public class Test {

    private final Sender sender;
    private final ForwardChainBuilder forwardChainBuilder;
    private final ReplyFetcher replyFetcher;

    @Value("${server.port}")
    private String port;

    @Value("${app.client-access-url}")
    private String clientAccessUrl;

    /**
     * 权限测试
     * @param payload
     */
    @DirectAuthInterceptor(
            scope = AuthScope.USER,
            mode  = AuthMode.ALLOW,
            targets = "1093664084"
    )
    public void quanxian(ParsedPayloadDTO payload) {
        sender.sendText(payload, "test: group not blacklisted -> passed");
    }

    /**
     * 多线程异步测试
     * 参数解析测试
     * @param payload
     */
    public void zuse(ParsedPayloadDTO payload, List<String> args) {
        long ms = 5000L;  // 默认延迟 5 秒
        try {
            if (args != null && !args.isEmpty()) {
                ms = Math.max(0, Long.parseLong(args.get(0)));
            }
        } catch (NumberFormatException ignore) {}

        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        sender.sendText(payload, "test: done after " + ms + " ms");
    }

    /**
     * 回复功能测试
     * @param payload
     */
    public void huifu(ParsedPayloadDTO payload) {
        sender.responseText(payload, "test");
    }

    /**
     * 图片发送测试
     */
    public void tu(ParsedPayloadDTO payload) {
        String url = "http://" + clientAccessUrl + ":" + port + "/saki.jpg";
        sender.responseImage(payload, List.of(url, url, url));
    }

    /**
     * 视频发送测试
     * @param payload
     */
    public void shipin(ParsedPayloadDTO payload) {
        String url = "http://" + clientAccessUrl + ":" + port + "/icsk.mp4";
        sender.sendVideo(payload, url);
    }

    /**
     * 生成转发消息测试
     * @param payload
     */
    public void zhuanfa(ParsedPayloadDTO payload, List<String> args) {
        System.out.println(args.toString());
        long idd = Long.parseLong(args.get(0));
        String json = forwardChainBuilder.create()
                .addRefNode(payload.getGroupId())
                .addCustomNode(idd, args.get(1), n -> n.text(args.get(2)))
                .toGroupJson(payload.getGroupId());

        sender.pushActionJSON(payload.getSelfId(), json);
    }

    /**
     * 测试 bot 获取非文本引用消息
     * @param payload
     */
    public void yinyong(ParsedPayloadDTO payload) {
        String replyMsgId = payload.getReplyToMessageId();
        if (replyMsgId == null || replyMsgId.isBlank()) {
            sender.sendText(payload, "请先『回复一条包含图片的消息』再发送 /test yinyong");
            return;
        }

        ReplayedMessagePayloadDTO r;
        try {
            r = replyFetcher.fetch(payload.getSelfId(), Long.parseLong(replyMsgId));
        } catch (Exception e) {
            sender.sendText(payload, "获取引用消息失败：" + e.getMessage());
            return;
        }
        if (r == null || r.getImages() == null || r.getImages().isEmpty()) {
            sender.sendText(payload, "引用的消息里没有图片（或实现未返回图片段）");
            return;
        }

        List<String> files = r.getImages().stream()
                .map(ImageRef::getSource)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (files.isEmpty()) {
            sender.sendText(payload, "引用的图片缺少可用的 url/file 字段");
            return;
        }

        sender.responseImage(payload, files);
    }

    /**
     * 测试 bot 获取参数
     * @param payload
     */
    public void canshu(ParsedPayloadDTO payload, List<String> args) {
        sender.responseText(payload, args.toString());
    }
}