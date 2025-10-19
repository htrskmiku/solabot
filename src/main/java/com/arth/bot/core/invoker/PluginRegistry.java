package com.arth.bot.core.invoker;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ForwardChainBuilder;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.plugins.Plugin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫描插件包 plugin 下带有 @BotPlugin 注解的 public 类及其带有 @BotCommand 注解的 public 方法，根据注解值建立命令映射
 * 绑定时将插件方法用 MethodHandle 封装为 CommandHandler，避免脆弱反射。
 */
@Slf4j
@Component
public class PluginRegistry {

    @Resource
    private Sender sender;
    @Resource
    private ForwardChainBuilder forwardChainBuilder;

    /**
     * 插件根包
     */
    private static final String PLUGIN_BASE_PACKAGE = "com.arth.bot.plugins";
    private final ApplicationContext applicationContext;
    /**
     * 模块别名（小写） -> 插件持有者
     */
    private final Map<String, PluginHolder> pluginRegistryMap = new ConcurrentHashMap<>();

    public PluginRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void init() {
        log.info("[core.invoker] scanning plugins under {}", PLUGIN_BASE_PACKAGE);
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(BotPlugin.class));
        for (var bd : scanner.findCandidateComponents(PLUGIN_BASE_PACKAGE)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                if (!java.lang.reflect.Modifier.isPublic(clazz.getModifiers())) continue;
                BotPlugin pluginAnn = clazz.getAnnotation(BotPlugin.class);
                if (pluginAnn == null) continue;
                // 创建未注册到容器的实例，手动注入 this，避免循环依赖
                Object instance = applicationContext.getAutowireCapableBeanFactory().createBean(clazz);
                if (instance instanceof Plugin plugin) plugin.setPluginRegistry(this);
                PluginHolder holder = new PluginHolder(instance);
                // 扫描 public 方法
                for (Method m : clazz.getMethods()) {
                    BotCommand cmdAnn = m.getAnnotation(BotCommand.class);
                    if (cmdAnn == null) continue;
                    if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
                    CommandHandler handler = createHandler(instance, m);
                    // 为每个别名注册（允许 "" / "index"）
                    for (String alias : cmdAnn.value()) {
                        String key = alias == null ? "" : alias.trim().toLowerCase(Locale.ROOT);
                        holder.addHandler(key, handler);
                        log.info("[core.invoker] command alias `{}` of plugin {} is registered", key, clazz.getSimpleName());
                    }
                }
                // 为每个模块别名注册
                for (String alias : pluginAnn.value()) {
                    String key = alias.trim().toLowerCase(Locale.ROOT);
                    if (pluginRegistryMap.putIfAbsent(key, holder) != null) {
                        log.warn("[core.invoker] duplicate plugin alias detected: {}", key);
                    }
                }
                log.info("[core.invoker] registered plugin: {} -> {}", Arrays.toString(pluginAnn.value()), clazz.getSimpleName());
            } catch (ClassNotFoundException e) {
                log.error("[core.invoker] class not found while scanning plugins", e);
            } catch (Throwable t) {
                log.error("[core.invoker] error creating plugin instance", t);
                throw t;
            }
        }
        if (pluginRegistryMap.isEmpty()) {
            log.warn("[core.invoker] no plugins found under {}", PLUGIN_BASE_PACKAGE);
        }
    }

    /**
     * 将插件方法绑定为 CommandHandler。
     * 完整支持六种旧签名（评分与优先级保持一致）：
     * (CommandChainContext, List) -> 6
     * (CommandChainContext)       -> 5
     * (ParsedPayloadDTO, List)    -> 4
     * (ParsedPayloadDTO)          -> 3
     * (List)                      -> 2
     * ()                          -> 1
     */
    private CommandHandler createHandler(Object instance, Method m) {
        Class<?>[] types = m.getParameterTypes();
        MethodHandles.Lookup lk = MethodHandles.lookup();
        try {
            // === () ===
            if (types.length == 0) {
                MethodHandle mh = lk.unreflect(m).bindTo(instance);
                return new CommandHandler() {
                    @Override
                    public Object handle(CommandChainContext chainCtx, ParsedPayloadDTO payload, List<String> args) throws Throwable {
                        return mh.invoke();
                    }

                    @Override
                    public int score(ParsedPayloadDTO payload, List<String> args) {
                        return 1;
                    }

                    @Override
                    public boolean acceptsArgs() {
                        return false;
                    }
                };
            }
            // === (ParsedPayloadDTO) ===
            if (types.length == 1 && types[0] == ParsedPayloadDTO.class) {
                MethodHandle mh = lk.unreflect(m).bindTo(instance)
                        .asType(MethodType.methodType(Object.class, ParsedPayloadDTO.class));
                return new CommandHandler() {
                    @Override
                    public Object handle(CommandChainContext chainCtx, ParsedPayloadDTO payload, List<String> args) throws Throwable {
                        return mh.invoke(payload);
                    }

                    @Override
                    public int score(ParsedPayloadDTO payload, List<String> args) {
                        return 3;
                    }

                    @Override
                    public boolean acceptsArgs() {
                        return false;
                    }
                };
            }
            // === (List) ===
            if (types.length == 1 && List.class.isAssignableFrom(types[0])) {
                MethodHandle mh = lk.unreflect(m).bindTo(instance)
                        .asType(MethodType.methodType(Object.class, List.class));
                return new CommandHandler() {
                    @Override
                    public Object handle(CommandChainContext chainCtx, ParsedPayloadDTO payload, List<String> args) throws Throwable {
                        return mh.invoke(args);
                    }

                    @Override
                    public int score(ParsedPayloadDTO payload, List<String> args) {
                        return 2;
                    }

                    @Override
                    public boolean acceptsArgs() {
                        return true;
                    }
                };
            }
            // === (ParsedPayloadDTO, List) ===
            if (types.length == 2 && types[0] == ParsedPayloadDTO.class && List.class.isAssignableFrom(types[1])) {
                MethodHandle mh = lk.unreflect(m).bindTo(instance)
                        .asType(MethodType.methodType(Object.class, ParsedPayloadDTO.class, List.class));
                return new CommandHandler() {
                    @Override
                    public Object handle(CommandChainContext chainCtx, ParsedPayloadDTO payload, List<String> args) throws Throwable {
                        return mh.invoke(payload, args);
                    }

                    @Override
                    public int score(ParsedPayloadDTO payload, List<String> args) {
                        return 4;
                    }

                    @Override
                    public boolean acceptsArgs() {
                        return true;
                    }
                };
            }
            // === (CommandChainContext) ===
            if (types.length == 1 && types[0] == CommandChainContext.class) {
                MethodHandle mh = lk.unreflect(m).bindTo(instance)
                        .asType(MethodType.methodType(Object.class, CommandChainContext.class));
                return new CommandHandler() {
                    @Override
                    public Object handle(CommandChainContext chainCtx, ParsedPayloadDTO payload, List<String> args) throws Throwable {
                        return mh.invoke(chainCtx);
                    }

                    @Override
                    public int score(ParsedPayloadDTO payload, List<String> args) {
                        return 5;
                    }

                    @Override
                    public boolean acceptsArgs() {
                        return false;
                    }
                };
            }
            // === (CommandChainContext, List) ===
            if (types.length == 2 && types[0] == CommandChainContext.class && List.class.isAssignableFrom(types[1])) {
                MethodHandle mh = lk.unreflect(m).bindTo(instance)
                        .asType(MethodType.methodType(Object.class, CommandChainContext.class, List.class));
                return new CommandHandler() {
                    @Override
                    public Object handle(CommandChainContext chainCtx, ParsedPayloadDTO payload, List<String> args) throws Throwable {
                        return mh.invoke(chainCtx, args);
                    }

                    @Override
                    public int score(ParsedPayloadDTO payload, List<String> args) {
                        return 6;
                    }

                    @Override
                    public boolean acceptsArgs() {
                        return true;
                    }
                };
            }
        } catch (Throwable e) {
            throw new InternalServerErrorException(
                    "Internal Server Error: failed to bind plugin method",
                    "服务器内部错误：无法绑定插件方法");
        }
        // 其余签名（如 3 参等）不支持，保持与旧版一致性 & 简洁性
        throw new InternalServerErrorException(
                "Internal Server Error: unsupported plugin method signature",
                "服务器内部错误：不支持的插件方法签名（仅支持 (), (Payload), (List), (Payload,List), (Ctx), (Ctx,List)）");
    }

    PluginHolder getPluginHolder(String pluginName) {
        return pluginRegistryMap.get(pluginName.toLowerCase(Locale.ROOT));
    }

    boolean contains(String pluginName) {
        return pluginRegistryMap.containsKey(pluginName.toLowerCase(Locale.ROOT));
    }

    public String getPluginHelpText(String pluginName) {
        Object aInstance = pluginRegistryMap.get(pluginName.toLowerCase(Locale.ROOT)).instance;
        return aInstance instanceof Plugin ? ((Plugin) aInstance).getHelpText() : null;
    }

    public void callPluginHelp(ParsedPayloadDTO payload, String pluginName) {
        Plugin aInstance = (Plugin) pluginRegistryMap.get(pluginName.toLowerCase(Locale.ROOT)).instance;
        String helpTextStr = aInstance.getHelpText();
        if (helpTextStr == null) {
            sender.replyText(payload, "不存在名为 \"" + pluginName + "\" 的插件/模块，请检查输入。");
            return;
        }

        ForwardChainBuilder building = forwardChainBuilder.create()
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("下面是 " + pluginName + " 模块的帮助文本"))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text(helpTextStr));

        String json = (payload.getGroupId() != null)
                ? building.toGroupJson(payload.getGroupId())
                : building.toPrivateJson(payload.getUserId());
        sender.pushActionJSON(payload.getSelfId(), json);
    }
}
