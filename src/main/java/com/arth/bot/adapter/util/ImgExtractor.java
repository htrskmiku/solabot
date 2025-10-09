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
import java.io.ByteArrayOutputStream;
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

    /**
     * 从 url 下载一张静态图片，返回 BufferedImage
     * @param aUrl
     * @return
     */
    public BufferedImage getBufferedImg(String aUrl) {
        try {
            URL url = new URL(aUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            try (InputStream inputStream = connection.getInputStream()) {
                return ImageIO.read(inputStream);
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 url 下载多张静态图片，返回 List<BufferedImage>
     * @param urls
     * @return
     */
    public List<BufferedImage> getBufferedImg(List<String> urls) {
        List<BufferedImage> imgs = new ArrayList<>();
        try {
            for (String url : urls) {
                imgs.add(getBufferedImg(url));
            }
        } catch (Exception e) {
            return List.of();
        }
        return imgs;
    }

    /**
     * 从 url 下载一张图片，返回二进制数据 byte[]
     * @param aUrl
     * @return
     */
    public byte[] getBytes(String aUrl) {
        try {
            URL url = new URL(aUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
                return baos.toByteArray();
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 url 下载多张图片，返回二进制数据 byte[][]
     * @param urls
     * @return
     */
    public byte[][] getBytes(List<String> urls) {
        List<byte[]> imgs = new ArrayList<>();
        try {
            for (String url : urls) {
                imgs.add(getBytes(url));
            }
        } catch (Exception e) {
            return new byte[0][];
        }
        return imgs.toArray(new byte[0][]);
    }

    /**
     * 从 OneBot v11 报文中提取图片媒体资源的 url
     * @param payload
     * @param printPrompt
     * @return
     */
    public List<String> extractImgUrls(ParsedPayloadDTO payload, boolean printPrompt) {
        String replyMsgId = payload.getReplyToMessageId();
        if (replyMsgId == null || replyMsgId.isBlank()) {
            if (printPrompt) sender.replyText(payload, "请引用一条图片消息哦");
            throw new InvalidCommandArgsException("", "未提取到引用消息");
        }

        ReplayedMessagePayloadDTO r;
        try {
            r = replyFetcher.fetch(payload.getSelfId(), Long.parseLong(replyMsgId));
        } catch (Exception e) {
            if (printPrompt) sender.replyText(payload, "获取引用消息失败：" + e.getMessage());
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
