package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.util.ImgExtractor;
import com.arth.bot.core.cache.service.CacheImageService;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component("plugins.img")
@BotPlugin({"img"})
@RequiredArgsConstructor
public class Img {

    private final Sender sender;
    private final ImgExtractor imgExtractor;
    private final CacheImageService cacheImageService;

    @Value("${server.port}")
    private String port;

    @Value("${app.client-access-url}")
    private String clientAccessUrl;

    private String baseUrl;

    @PostConstruct
    public void init() {
        this.baseUrl = "http://" + clientAccessUrl + ":" + port;
    }

    public void index(ParsedPayloadDTO payload) {
        sender.sendText(payload, "具体的图片处理命令是什么呢？");
    }

    public void mid(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImgs(urls);

        for (BufferedImage img : imgs) {
            int width = img.getWidth();
            int height = img.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width / 2; x++) {
                    int leftPixel = img.getRGB(x, y);
                    img.setRGB(width - x - 1, y, leftPixel);
                }
            }
        }

        List<String> uuids = cacheImageService.cachePngImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/" + uuid);
        sender.sendImage(payload, cacheUrls);
    }

    public void mid(ParsedPayloadDTO payload, List<String> args) throws IOException {
        if (args == null || args.isEmpty() || args.get(0).equals("l")) {
            mid(payload);
            return;
        }

        if (!args.get(0).equals("r")) {
            sender.replyText(payload, "mid 命令支持的参数是 l 或 r，默认 l");
            return;
        }

        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImgs(urls);

        for (BufferedImage img : imgs) {
            int width = img.getWidth();
            int height = img.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width / 2; x++) {
                    int rightPixel = img.getRGB(width - x - 1, y);
                    img.setRGB(x, y, rightPixel);
                }
            }
        }

        List<String> uuids = cacheImageService.cachePngImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/" + uuid);
        sender.sendImage(payload, cacheUrls);
    }

    public void mirror(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImgs(urls);

        for (BufferedImage img : imgs) {
            int width = img.getWidth();
            int height = img.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width / 2; x++) {
                    int leftPixel = img.getRGB(x, y);
                    int rightPixel = img.getRGB(width - x - 1, y);

                    img.setRGB(x, y, rightPixel);
                    img.setRGB(width - x - 1, y, leftPixel);
                }
            }
        }

        List<String> uuids = cacheImageService.cachePngImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/" + uuid);
        sender.sendImage(payload, cacheUrls);
    }

    public void gray(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImgs(urls);

        for (int i = 0; i < imgs.size(); i++) {
            BufferedImage img = imgs.get(i);
            int width = img.getWidth();
            int height = img.getHeight();

            BufferedImage grayImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics g = grayImg.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            imgs.set(i, grayImg);
        }

        List<String> uuids = cacheImageService.cachePngImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/" + uuid);
        sender.sendImage(payload, cacheUrls);
    }
}