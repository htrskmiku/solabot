package com.arth.bot.adapter.util;

import com.arth.bot.adapter.fetcher.ReplyFetcher;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.dto.ReplayedMessagePayloadDTO;
import com.arth.bot.core.common.dto.replay.ImageRef;
import com.arth.bot.core.common.exception.InvalidCommandArgsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ImgExtractor {

    private final Sender sender;
    private final ReplyFetcher replyFetcher;

    public List<BufferedImage> getImgs(List<String> urls) {
        List<BufferedImage> imgs = new ArrayList<>();

        try {
            for (String urlStr : urls) {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                try (InputStream inputStream = connection.getInputStream()) {
                    imgs.add(ImageIO.read(inputStream));
                } finally {
                    connection.disconnect();
                }
            }
        } catch (Exception e) {
            return List.of();
        }

        return imgs;
    }

    public List<String> extractImgUrls(ParsedPayloadDTO payload, boolean printPrompt) {
        String replyMsgId = payload.getReplyToMessageId();
        if (replyMsgId == null || replyMsgId.isBlank()) {
            if (printPrompt) sender.responseText(payload, "请引用一条图片消息哦");
            throw new InvalidCommandArgsException("", "未提取到引用消息");
        }

        ReplayedMessagePayloadDTO r;
        try {
            r = replyFetcher.fetch(payload.getSelfId(), Long.parseLong(replyMsgId));
        } catch (Exception e) {
            if (printPrompt) sender.responseText(payload, "获取引用消息失败：" + e.getMessage());
            return List.of();
        }
        if (r == null || r.getImages() == null || r.getImages().isEmpty()) {
            if (printPrompt) sender.sendText(payload, "引用的消息里没有图片（或实现未返回图片段）");
            return List.of();
        }

        List<String> urls = r.getImages().stream()
                .map(ImageRef::getSource)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (urls.isEmpty()) {
            if (printPrompt) sender.sendText(payload, "引用的图片缺少可用的 url/file 字段");
            return List.of();
        }

        return urls;
    }

    public List<String> extractImgUrls(ParsedPayloadDTO payload) {
        return extractImgUrls(payload, false);
    }
}
