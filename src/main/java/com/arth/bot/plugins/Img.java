package com.arth.bot.plugins;

import com.arth.bot.adapter.fetcher.ReplyFetcher;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.util.ImgExtractor;
import com.arth.bot.core.cache.service.CacheImageService;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.dto.ReplayedMessagePayloadDTO;
import com.arth.bot.core.common.dto.replay.ImageRef;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    private final String baseUrl = "http://" + clientAccessUrl + ":" + port;

    public void index(ParsedPayloadDTO payload) {
        sender.sendText(payload, "具体的图片处理命令是什么呢？");
    }

    public void mid(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
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
        sender.replyImage(payload, cacheUrls);
    }

    public void gray(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        List<BufferedImage> imgs = imgExtractor.getBufferedImgs(urls);

        for (BufferedImage img : imgs) {
            int width = img.getWidth();
            int height = img.getHeight();

            BufferedImage grayImg = new BufferedImage(
                    width,
                    height,
                    BufferedImage.TYPE_BYTE_GRAY
            );

            Graphics g = grayImg.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img.setData(grayImg.getRaster());
        }

        List<String> uuids = cacheImageService.cachePngImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/" + uuid);
        sender.replyImage(payload, cacheUrls);
    }
}