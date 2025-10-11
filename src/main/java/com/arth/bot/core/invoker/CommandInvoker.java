package com.arth.bot.core.invoker;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.BusinessException;
import com.arth.bot.core.common.exception.CommandNotFoundException;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.common.exception.InvalidCommandArgsException;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.plugins.DefaultStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.reflect.Modifier.isPublic;

/**
 * CommandInvoker — 使用 CommandHolder 与别名表实现一次性初始化、按别名路由、并保留签名优先级选择
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandInvoker {

    private final ApplicationContext ctx;
    private final DefaultStrategy defaultStrategy;

    /* 字符串到插件 Bean 的全局映射 */
    private final Map<String, CommandHolder> pluginRegistry = new ConcurrentHashMap<>();
    /* 命令优先级缓存 */
    private final Map<CommandHolder, Map<String, Method>> preferredMethods = new ConcurrentHashMap<>();

    /**
     * 注册插件 Bean 别名，为 Bean 注册命令别名
     */
    @PostConstruct
    public void init() {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(BotPlugin.class);
        for (Object bean : beans.values()) {

            // 拿到动态代理前的类
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            String className = targetClass.getSimpleName();
            String pluginName = className.toLowerCase(Locale.ROOT);

            CommandHolder holder = new CommandHolder(bean);
            // 小心动态代理抹掉了类注解
            BotPlugin pluginAnno = AnnotationUtils.findAnnotation(bean.getClass(), BotPlugin.class);
            String[] pluginAliases = pluginAnno.value();
            if (pluginAliases == null) continue;

            for (String pluginAlias : pluginAliases) {
                if (pluginAlias == null) continue;
                pluginRegistry.put(pluginAlias.toLowerCase(Locale.ROOT), holder);
            }

            pluginRegistry.put(pluginName, holder);

            Map<String, List<Method>> cmdRegistry = holder.commandRegistry;
            for (Method m : targetClass.getDeclaredMethods()) {
                if (!isPublic(m.getModifiers())) continue;

                // 同样地，小心动态代理抹掉了方法注解
                BotCommand cmdAnno = AnnotationUtils.findAnnotation(m, BotCommand.class);

                if (cmdAnno != null) {
                    String[] cmdAliases = cmdAnno.value();
                    for (String cmdAlias : cmdAliases) {
                        String alias = cmdAlias.toLowerCase(Locale.ROOT);
                        cmdRegistry.computeIfAbsent(alias, key -> new ArrayList<>()).add(m);
                        log.info("[core.invoker] command alias `{}` of plugin {} is registered", alias, pluginName);
                    }
                }

                // 合成方法、桥接方法、内部方法通通不注册
                String nameKey = m.getName().toLowerCase(Locale.ROOT);
                if (!m.isSynthetic() && !m.isBridge() && !nameKey.contains("$")) {
                    cmdRegistry.computeIfAbsent(nameKey, key -> new ArrayList<>()).add(m);
                    log.info("[core.invoker] command alias `{}` of plugin {} is registered", nameKey, pluginName);
                }
            }

            Map<String, Method> bestMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, List<Method>> e : cmdRegistry.entrySet()) {
                String alias = e.getKey();
                List<Method> methods = e.getValue();
                if (methods == null || methods.isEmpty()) continue;

                methods.sort(Comparator.comparingInt(CommandInvoker::scoreSignature).reversed());
                bestMap.put(alias, methods.get(0));
            }
            preferredMethods.put(holder, Collections.unmodifiableMap(bestMap));

            log.info("[core.invoker] registered plugin `{}` commands aliases", pluginName);
        }
        log.info("[core.invoker] total {} plugin aliases registered", pluginRegistry.size());
    }

    /**
     * 命令优先级得分
     * - 6分：(CommandChainContext, List)，链式操作
     * - 5分：(CommandChainContext)
     * - 4分：(ParsedPayloadDTO, List)
     * - 3分：(ParsedPayloadDTO)
     * - 2分：(List)
     * - 1分：()
     * - 0分：未匹配的签名
     * @param m
     * @return
     */
    private static int scoreSignature(Method m) {
        Class<?>[] ps = m.getParameterTypes();
        if (ps.length == 2 && ps[0] == CommandChainContext.class && List.class.isAssignableFrom(ps[1])) {
            return 6;
        } else if (ps.length == 1 && ps[0] == CommandChainContext.class) {
            return 5;
        } else if (ps.length == 2 && ps[0] == ParsedPayloadDTO.class && List.class.isAssignableFrom(ps[1])) {
            return 4;
        } else if (ps.length == 1 && ps[0] == ParsedPayloadDTO.class) {
            return 3;
        } else if (ps.length == 1 && List.class.isAssignableFrom(ps[0])) {
            return 2;
        } else if (ps.length == 0) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 命令解析入口
     * @param payload
     * @return
     */
    public Object parseAndInvoke(ParsedPayloadDTO payload) {
        String line = payload.getCommandText();

        if (line == null || line.isBlank()) {
            if (defaultStrategy != null && "message".equals(payload.getPostType())) {
                defaultStrategy.defaultHandle(payload);
            }
            return null;
        }

        String lline = line.stripLeading();
        if (lline.isEmpty() || lline.charAt(0) != '/') {
            if (defaultStrategy != null) defaultStrategy.defaultHandle(payload);
            return null;
        }
        Parsed parsed = parse(lline);
        // 通过插件别名表直接拿到 holder（避免每次反射遍历）
        CommandHolder holder = getPluginHolder(parsed.root());
        Object bean = holder.bean;

        // 将参数序列按已知方法名进行分组，形成执行步骤链
        List<Step> steps = groupSteps(parsed.subAndArgs(), holder);
        if (steps.isEmpty()) {
            // 无明确子命令时，默认执行 index 方法作为入口
            steps = List.of(new Step("index", List.of()));
        }

        // 创建命令链执行上下文，贯穿整个执行生命周期
        CommandChainContext chainCtx = new CommandChainContext(payload);

        // 顺序执行命令链中的每个步骤
        Object lastRet = null;
        for (Step s : steps) {
            // 解析当前步骤对应的方法（基于 holder 的别名 -> 方法映射，并考虑签名优先级）
            Method m = resolveMethod(holder, s.name());
            // 执行当前步骤方法，传入上下文和参数
            lastRet = invokeStep(bean, m, chainCtx, s.args());
            // 方法返回值非空时，自动设置为上下文状态供后续步骤使用
            if (lastRet != null) chainCtx.setState(lastRet);
        }

        return lastRet;
    }

    /**
     * 初步处理文本，提取首行的命令字符串
     * @param payload
     * @return
     */
    @Deprecated
    private String extractLeadingSlashCommand(ParsedPayloadDTO payload) {
        String rowText = payload.getRawText();
        if (rowText == null || rowText.isEmpty()) return null;
        if (!rowText.startsWith("/")) {
            if (defaultStrategy != null) defaultStrategy.defaultHandle(payload);
            return null;
        }
        String text = rowText.trim();  // String text = Optional.of(rowText).orElse("").trim();
        int nl = text.indexOf('\n');
        return nl >= 0 ? text.substring(0, nl).trim() : text;
    }

    /**
     * 解析命令字符串，返回 Parsed 对象（根命令（首个 token）与 tokens，tokens 包含子命令与参数）
     * @param raw
     * @return
     */
    private Parsed parse(String raw) {
        String s = raw.trim().replaceAll("\\s+", " ").replaceAll("^/+", "/");
        String cmd = s.substring(1).trim();

        List<String> tokens = new ArrayList<>(Arrays.asList(cmd.split(" ")));
        if (tokens.isEmpty()) throw new InvalidCommandArgsException("[core.invoker] invalid command: root command in need", "命令格式错误: 缺少根命令");
        String root = tokens.remove(0);
        log.debug("[core.invoker] command detected: {}", root);
        return new Parsed(root, tokens);
    }

    /**
     * 进一步解析命令字符串，将 tokens 解析为子命令与对应的参数
     * 规则：当遇到一个 token 在 methodNames 中时，判定当前正在收集参数的命令是否允许接受参数；
     * 如果允许，则把该 token 当作参数；
     * 否则将其视为新的命令名。
     * @param tokens
     * @param holder
     * @return
     */
    private List<Step> groupSteps(List<String> tokens, CommandHolder holder) {
        List<Step> out = new ArrayList<>();
        if (tokens == null || tokens.isEmpty()) return out;

        String curName = null;
        List<String> curArgs = new ArrayList<>();

        Set<String> methodNames = holder.commandRegistry.keySet();

        for (int idx = 0; idx < tokens.size(); idx++) {
            String tk = tokens.get(idx);
            String low = tk.toLowerCase(Locale.ROOT);

            if (methodNames.contains(low)) {
                // 当前命令不接收参数，贪心匹配新的命令
                if (curName == null) {
                    curName = tk;
                    curArgs.clear();
                    continue;
                }

                // 当前已有命令，判断其方法签名是否允许接收参数 List<String> args
                Method currentMethod;
                try {
                    // 根据优先级匹配最适命令
                    currentMethod = resolveMethod(holder, curName);
                } catch (CommandNotFoundException e) {
                    out.add(new Step(curName, List.copyOf(curArgs)));
                    curName = tk;
                    curArgs = new ArrayList<>();
                    continue;
                }

                boolean acceptsList = false;
                Class<?>[] ps = currentMethod.getParameterTypes();
                for (Class<?> p : ps) {
                    if (List.class.isAssignableFrom(p)) {
                        acceptsList = true;
                        break;
                    }
                }

                if (acceptsList) {
                    curArgs.add(tk);
                } else {
                    out.add(new Step(curName, List.copyOf(curArgs)));
                    curName = tk;
                    curArgs = new ArrayList<>();
                }
            } else {
                curArgs.add(tk);
            }
        }

        if (curName != null) out.add(new Step(curName, List.copyOf(curArgs)));
        return out;
    }

    /**
     * 从 pluginRegistry 中获取插件 Holder（使用别名表，不再直接从 Spring 容器按 beanName 获取）
     * @param root
     * @return CommandHolder
     */
    private CommandHolder getPluginHolder(String root) {
        if (root == null) {
            throw new CommandNotFoundException("[core.invoker] unmatched root command: null", "未匹配到命令模块: null");
        }
        String key = root.toLowerCase(Locale.ROOT);
        CommandHolder holder = pluginRegistry.get(key);
        if (holder == null) {
            throw new CommandNotFoundException("[core.invoker] unmatched root command: " + key, "未匹配到命令模块: " + key);
        }
        return holder;
    }

    /**
     * 结合命令优先级，从 holder 提供的别名 -> 方法列表中返回优先级最高的方法
     * @param holder
     * @param sub  子命令文本（原始大小写），内部会进行小写匹配
     * @return 最优 Method
     */
    private Method resolveMethod(CommandHolder holder, String sub) {
        String key = sub == null ? "" : sub.toLowerCase(Locale.ROOT);

        // 先查 init 时缓存的 preferredMethods
        Map<String, Method> bestMap = preferredMethods.get(holder);
        if (bestMap != null && bestMap.containsKey(key)) {
            return bestMap.get(key);
        }

        // 回退：从 holder.commandRegistry 中取候选并按 scoreSignature 决定，再缓存（线程安全地）
        List<Method> candidates = holder.commandRegistry.get(key);
        if (candidates == null || candidates.isEmpty()) {
            throw new CommandNotFoundException("[core.invoker] command not supported: " + sub, "不支持的命令: " + sub);
        }

        Method best = candidates.stream()
                .max(Comparator.comparingInt(CommandInvoker::scoreSignature))
                .orElseThrow(() -> new CommandNotFoundException("[core.invoker] command not supported: " + sub, "不支持的命令: " + sub));

        // 把选出的最佳方法缓存到 preferredMethods（构造并放入 holder 对应 map）
        preferredMethods.computeIfAbsent(holder, h -> new ConcurrentHashMap<>());
        // 如果原来是不可变 map，我们替换为可变 map（第一次回退路径），以便后续直接复用；注意：这一步只在极少数情况发生（如果 init 没缓存到）
        Map<String, Method> mapRef = preferredMethods.get(holder);
        if (!(mapRef instanceof ConcurrentHashMap)) {
            // 覆盖为可变实现（保守操作）
            Map<String, Method> newMap = new ConcurrentHashMap<>(mapRef);
            preferredMethods.put(holder, newMap);
            mapRef = preferredMethods.get(holder);
        }
        mapRef.put(key, best);
        return best;
    }

    /**
     * 执行单个命令的方法调用
     * @param bean
     * @param m
     * @param chainCtx
     * @param args
     * @return
     */
    private Object invokeStep(Object bean, Method m, CommandChainContext chainCtx, List<String> args) {
        try {
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 2) {
                if (ps[0] == CommandChainContext.class) return m.invoke(bean, chainCtx, args);
                else return m.invoke(bean, chainCtx.getPayload(), args);
            } else if (ps.length == 1) {
                if (ps[0] == CommandChainContext.class) return m.invoke(bean, chainCtx);
                else if (ps[0] == ParsedPayloadDTO.class)
                    return m.invoke(bean, chainCtx.getPayload());
                else return m.invoke(bean, args);
            } else {
                return m.invoke(bean);
            }
        } catch (IllegalAccessException e) {
            log.error("[core.invoker] unable to invoke method: " + m.getName(), e);
            throw new InternalServerErrorException("Internal Server Error: " + e.getMessage(), "服务器内部错误；非法的反射调用");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException) {
                throw (BusinessException) cause;
            }
            log.error("[core.invoker] unable to invoke method: " + m.getName(), e);
            throw new InternalServerErrorException("Internal Server Error: " + cause.getMessage(), "服务器内部错误：未知的反射执行时异常");
        }
    }

    /**
     * Parsed 对象，组合根命令与子命令
     * @param root
     * @param subAndArgs
     */
    private record Parsed(String root, List<String> subAndArgs) {
    }

    /**
     * Step 对象，组合（子命令）与附加参数
     * @param name
     * @param args
     */
    private record Step(String name, List<String> args) {
    }
}
