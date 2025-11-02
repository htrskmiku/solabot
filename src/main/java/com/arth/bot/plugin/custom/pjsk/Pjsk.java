package com.arth.bot.plugin.custom.pjsk;

import com.arth.bot.adapter.controller.ApiPaths;
import com.arth.bot.adapter.fetcher.http.ImgService;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ActionChainBuilder;
import com.arth.bot.core.cache.service.ImageCacheService;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.core.database.mapper.PjskBindingMapper;
import com.arth.bot.plugin.custom.Plugin;
import com.arth.bot.plugin.custom.pjsk.func.Mysekai;
import com.arth.bot.plugin.custom.pjsk.func.Suite;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@BotPlugin({"pjsk"})
@RequiredArgsConstructor
public class Pjsk extends Plugin {

    @Getter
    public final String helpText = "请通过 /help 查看 pjsk 模块具体的命令";

    @BotCommand("index")
    @Override
    public void index(ParsedPayloadDTO payload) {
        sender.replyText(payload, "请接 pjsk 模块的具体命令哦");
    }

    @BotCommand("help")
    @Override
    public void help(ParsedPayloadDTO payload) {
        sender.replyText(payload, helpText);
    }

    // ***** ============= Suite ============= *****

    @BotCommand({"box", "卡牌一览"})
    public void box(ParsedPayloadDTO payload,List<String> args) {
        Suite.box(getCtx(), payload,args);
    }



    // ***** ============= Mysekai ============= *****

    @BotCommand({"绑定", "bind"})
    public void bind(ParsedPayloadDTO payload, List<String> args) {
        Mysekai.bind(getCtx(), payload, args);
    }

    @BotCommand({"查询绑定", "bound"})
    public void bound(ParsedPayloadDTO payload) {
        Mysekai.bound(getCtx(), payload);
    }

    @BotCommand({"msm", "msr"})
    public void msm(ParsedPayloadDTO payload) {
        Mysekai.msm(getCtx(), payload);
    }

    @BotCommand({"msm", "msr"})
    public void msm(ParsedPayloadDTO payload, List<String> args) {
        if (args == null || args.isEmpty()) {
            msm(payload);
        } else {
            Mysekai.msm(getCtx(), payload, args.get(0));
        }
    }

    // ***** ============= ctx ============= *****
    // ***** ============= ctx ============= *****
    // ***** ============= ctx ============= *****

    private volatile CoreBeanContext ctx;
    private final Sender sender;
    private final WebClient webClient;
    private final ApiPaths apiPaths;
    private final ActionChainBuilder actionChainBuilder;
    private final ImageCacheService imageCacheService;
    private final PjskBindingMapper pjskBindingMapper;
    private final ObjectMapper objectMapper;
    private final ImgService imgService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Value("${app.local-path.pjsk-resource.dynamic.mysekai.map}")
    String localMapPath;
    @Value("${app.local-path.pjsk-resource.dynamic.mysekai.overview}")
    String localOverviewPath;
    @Value("${app.local-path.pjsk-resource.static.master-data.root}")
    private String masterDataPath;
    @Value("${app.local-path.pjsk-resource.static.master-data.cards}")
    private String cardsDataPath;
    @Value("${app.parameter.plugin.pjsk.external-api.hrk.suite-api}")
    private String suiteApi;
    @Value("${app.parameter.plugin.pjsk.external-api.hrk.mysekai-api}")
    private String mysekaiApi;
    @Value("${app.parameter.plugin.pjsk.external-api.uni.thumbnail-api}")
    private String thumbnailApi;

    @Value("${app.parameter.plugin.pjsk.devel_mode}")
    private boolean devel_mode;
    @Value("${app.parameter.plugin.pjsk.cache_cards_thumbnails}")
    private boolean cache_cards_thumbnails;

    /**
     * 懒汉式线程安全获取 ctx
     *
     * @return
     */
    public CoreBeanContext getCtx() {
        if (ctx == null) {
            synchronized (this) {
                if (ctx == null) {
                    ctx = new CoreBeanContext(
                            sender,
                            webClient,
                            apiPaths,
                            actionChainBuilder,
                            imageCacheService,
                            pjskBindingMapper,
                            objectMapper,
                            imgService,
                            dateTimeFormatter,
                            Path.of(localMapPath).toAbsolutePath().normalize(),
                            Path.of(localOverviewPath).toAbsolutePath().normalize(),
                            Path.of(masterDataPath).toAbsolutePath().normalize(),
                            Path.of(cardsDataPath).toAbsolutePath().normalize(),
                            suiteApi,
                            mysekaiApi,
                            thumbnailApi,
                            devel_mode,
                            cache_cards_thumbnails
                    );
                }
            }
        }
        return ctx;
    }

    public interface BeanContext {
        Sender sender();
        WebClient webClient();
        ApiPaths apiPaths();
        ActionChainBuilder actionChainBuilder();
        ImageCacheService imageCacheService();
        PjskBindingMapper pjskBindingMapper();
        ObjectMapper objectMapper();
        ImgService imgService();
        DateTimeFormatter dateTimeFormatter();
        Path localMapPath();
        Path localOverviewPath();
        Path masterDataPath();
        Path cardsDataPath();
        String suiteApi();
        String mysekaiApi();
        String thumbnailApi();
        boolean devel_mode();
        boolean cache_cards_thumbnails();
    }

    public record CoreBeanContext(
            Sender sender,
            WebClient webClient,
            ApiPaths apiPaths,
            ActionChainBuilder actionChainBuilder,
            ImageCacheService imageCacheService,
            PjskBindingMapper pjskBindingMapper,
            ObjectMapper objectMapper,
            ImgService imgService,
            DateTimeFormatter dateTimeFormatter,
            Path localMapPath,
            Path localOverviewPath,
            Path masterDataPath,
            Path cardsDataPath,
            String suiteApi,
            String mysekaiApi,
            String thumbnailApi,
            boolean devel_mode,
            boolean cache_cards_thumbnails) implements BeanContext {
    }
}