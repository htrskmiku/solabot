package com.arth.bot.plugins.pjsk;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ActionChainBuilder;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.core.database.mapper.PjskBindingMapper;
import com.arth.bot.plugins.Plugin;
import com.arth.bot.plugins.pjsk.helper.Mysekai;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@BotPlugin({"pjsk"})
@RequiredArgsConstructor
public class Pjsk extends Plugin {

    private final Sender sender;
    private final ActionChainBuilder actionChainBuilder;
    private final PjskBindingMapper pjskBindingMapper;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Value("${app.client-access-network-endpoint}")
    String networkEndpoint;
    @Value("${app.api-path.plugin.pjsk.root}")
    String rootPath;
    @Value("${app.api-path.plugin.pjsk.mysekai.map}")
    String mapPath;
    @Value("${app.api-path.plugin.pjsk.mysekai.overview}")
    String overviewPath;
    @Value("${app.local-path.pjsk-mysekai.map}")
    String localMapPath;

    private volatile CoreBeanContext ctx;


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
                            actionChainBuilder,
                            pjskBindingMapper,
                            dateTimeFormatter,
                            networkEndpoint,
                            rootPath,
                            mapPath,
                            overviewPath,
                            localMapPath
                    );
                }
            }
        }
        return ctx;
    }

    public interface BeanContext {
        Sender sender();
        ActionChainBuilder actionChainBuilder();
        PjskBindingMapper pjskBindingMapper();
        DateTimeFormatter dateTimeFormatter();
        String networkEndpoint();
        String rootPath();
        String mapPath();
        String overviewPath();
        String localMapPath();
    }

    public record CoreBeanContext(
            Sender sender,
            ActionChainBuilder actionChainBuilder,
            PjskBindingMapper pjskBindingMapper,
            DateTimeFormatter dateTimeFormatter,
            String networkEndpoint,
            String rootPath,
            String mapPath,
            String overviewPath,
            String localMapPath) implements BeanContext {
    }
}