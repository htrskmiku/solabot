package com.arth.bot.plugins;

import com.arth.bot.adapter.fetcher.ReplyFetcher;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.util.ImgExtractor;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.dto.ReplayedMessagePayloadDTO;
import com.arth.bot.core.common.dto.replay.ImageRef;
import com.arth.bot.core.invoker.annotation.BotPlugin;
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

@Component("plugins.img")
@BotPlugin({"img"})
@RequiredArgsConstructor
public class Img {

    private final Sender sender;
    private final ImgExtractor imgExtractor;

    public void index(ParsedPayloadDTO payload) {
        sender.sendText(payload, "具体的命令是什么呢？");
    }

    public void mid(ParsedPayloadDTO payload) {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);

    }
}