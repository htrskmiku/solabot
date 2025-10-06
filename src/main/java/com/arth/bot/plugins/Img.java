package com.arth.bot.plugins;

import com.arth.bot.adapter.fetcher.ReplyFetcher;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.dto.ReplayedMessagePayloadDTO;
import com.arth.bot.core.common.dto.replay.ImageRef;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component("plugins.img")
@BotPlugin({"img"})
@RequiredArgsConstructor
public class Img {

    private final Sender sender;
    private final ReplyFetcher replyFetcher;

    public void index(ParsedPayloadDTO payload) {
        sender.sendText(payload, "具体的命令是什么呢？");
    }

    public void mid(ParsedPayloadDTO payload) {
        List<String> imgUrls = extractImgUrls(payload);
        for (String url : imgUrls) {

        }
        sender.responseImage(payload, imgUrls);
    }

    private List<String> extractImgUrls(ParsedPayloadDTO payload) {
        String replyMsgId = payload.getReplyToMessageId();
        if (replyMsgId == null || replyMsgId.isBlank()) {
            sender.responseText(payload, "请引用一条图片消息哦");
            return List.of();
        }

        ReplayedMessagePayloadDTO r;
        try {
            r = replyFetcher.fetch(payload.getSelfId(), Long.parseLong(replyMsgId));
        } catch (Exception e) {
            sender.responseText(payload, "获取引用消息失败：" + e.getMessage());
            return List.of();
        }
        if (r == null || r.getImages() == null || r.getImages().isEmpty()) {
            sender.sendText(payload, "引用的消息里没有图片（或实现未返回图片段）");
            return List.of();
        }

        List<String> files = r.getImages().stream()
                .map(ImageRef::getSource)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (files.isEmpty()) {
            sender.sendText(payload, "引用的图片缺少可用的 url/file 字段");
            return List.of();
        }

        return files;
    }
}