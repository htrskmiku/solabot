package com.arth.bot.plugin.custom.pjsk;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ActionChainBuilder;
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
    public void box(ParsedPayloadDTO payload) {
        Suite.box(getCtx(), payload);
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

    // ***** ============= ctx ============= *****
    // ***** ============= ctx ============= *****
    // ***** ============= ctx ============= *****

    private volatile CoreBeanContext ctx;

    private final Sender sender;
    private final WebClient webClient;
    private final ActionChainBuilder actionChainBuilder;
    private final PjskBindingMapper pjskBindingMapper;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Value("${app.client-access-network-endpoint}")
    String networkEndpoint;
    @Value("${app.api-path.plugin.pjsk.root}")
    String rootApiPath;
    @Value("${app.api-path.plugin.pjsk.mysekai.map}")
    String mapApiPath;
    @Value("${app.api-path.plugin.pjsk.mysekai.overview}")
    String overviewApiPath;
    @Value("${app.local-path.pjsk-resource.dynamic.mysekai.map}")
    String localMapPath;
    @Value("${app.local-path.pjsk-resource.dynamic.mysekai.overview}")
    String localOverviewPath;
    @Value("${app.local-path.pjsk-resource.static.master-data.root}")
    private String masterDataPath;
    @Value("${app.parameter.plugin.pjsk.external-api.hrk.suite-api}")
    private String suiteApi;
    @Value("${app.parameter.plugin.pjsk.external-api.hrk.mysekai-api}")
    private String mysekaiApi;
    @Value("${app.parameter.plugin.pjsk.external-api.uni.thumbnail-api}")
    private String thumbnailApi;

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
                            actionChainBuilder,
                            pjskBindingMapper,
                            objectMapper,
                            dateTimeFormatter,
                            networkEndpoint,
                            rootApiPath,
                            mapApiPath,
                            overviewApiPath,
                            Path.of(localMapPath).toAbsolutePath().normalize(),
                            Path.of(localOverviewPath).toAbsolutePath().normalize(),
                            Path.of(masterDataPath).toAbsolutePath().normalize(),
                            suiteApi,
                            mysekaiApi,
                            thumbnailApi
                    );
                }
            }
        }
        return ctx;
    }

    public interface BeanContext {
        Sender sender();
        WebClient webClient();
        ActionChainBuilder actionChainBuilder();
        PjskBindingMapper pjskBindingMapper();
        ObjectMapper objectMapper();
        DateTimeFormatter dateTimeFormatter();
        String networkEndpoint();
        String rootApiPath();
        String mapApiPath();
        String overviewApiPath();
        Path localMapPath();
        Path localOverviewPath();
        Path masterDataPath();
        String suiteApi();
        String mysekaiApi();
        String thumbnailApi();
    }

    public record CoreBeanContext(
            Sender sender,
            WebClient webClient,
            ActionChainBuilder actionChainBuilder,
            PjskBindingMapper pjskBindingMapper,
            ObjectMapper objectMapper,
            DateTimeFormatter dateTimeFormatter,
            String networkEndpoint,
            String rootApiPath,
            String mapApiPath,
            String overviewApiPath,
            Path localMapPath,
            Path localOverviewPath,
            Path masterDataPath,
            String suiteApi,
            String mysekaiApi,
            String thumbnailApi) implements BeanContext {
    }
}