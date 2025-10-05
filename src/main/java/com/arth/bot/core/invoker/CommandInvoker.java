package com.arth.bot.core.invoker;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.BusinessException;
import com.arth.bot.core.common.exception.CommandNotFoundException;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.common.exception.InvalidCommandArgsException;
import com.arth.bot.plugins.DefaultStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandInvoker {

    private final ApplicationContext ctx;
    private final DefaultStrategy defaultStrategy;

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
        } else if (ps.length == 2 && ps[0] == com.arth.bot.core.common.dto.ParsedPayloadDTO.class && List.class.isAssignableFrom(ps[1])) {
            return 4;
        } else if (ps.length == 1 && ps[0] == com.arth.bot.core.common.dto.ParsedPayloadDTO.class) {
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
        Object bean = getPluginBean(parsed.root());

        // 收集插件Bean所有可用方法名（忽略大小写），用于后续命令识别
        Set<String> methodNames = Arrays.stream(bean.getClass().getMethods())
                .map(Method::getName).map(n -> n.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        // 将参数序列按已知方法名进行分组，形成执行步骤链
        List<Step> steps = groupSteps(parsed.subAndArgs(), methodNames);
        if (steps.isEmpty()) {
            // 无明确子命令时，默认执行 index 方法作为入口
            steps = List.of(new Step("index", List.of()));
        }

        // 创建命令链执行上下文，贯穿整个执行生命周期
        CommandChainContext chainCtx = new CommandChainContext(payload);

        // 顺序执行命令链中的每个步骤
        Object lastRet = null;
        for (Step s : steps) {
            // 解析当前步骤对应的方法
            Method m = resolveMethod(bean, s.name());
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
        if (tokens.isEmpty()) throw new InvalidCommandArgsException("[core] invalid command: root command in need", "命令格式错误: 缺少根命令");
        String root = tokens.remove(0);
        log.debug("[core] command detected: {}", root);
        return new Parsed(root, tokens);
    }

    /**
     * 进一步解析命令字符串，将 tokens 解析为子命令与对应的参数
     * @param tokens
     * @param methodNames
     * @return
     */
    private List<Step> groupSteps(List<String> tokens, Set<String> methodNames) {
        List<Step> out = new ArrayList<>();
        if (tokens.isEmpty()) return out;

        String curName = null;
        List<String> curArgs = new ArrayList<>();

        for (String tk : tokens) {
            String low = tk.toLowerCase(Locale.ROOT);
            if (methodNames.contains(low)) {
                // 新方法名，规约前一方法的参数
                if (curName != null) out.add(new Step(curName, List.copyOf(curArgs)));
                curName = tk;
                curArgs.clear();
            } else {
                // 非方法名，规约为当前方法的参数
                curArgs.add(tk);
            }
        }
        if (curName != null) out.add(new Step(curName, List.copyOf(curArgs)));
        return out;
    }

    /**
     * 从 Spring 容器中获取根命令的插件 Bean
     * @param root
     * @return
     */
    private Object getPluginBean(String root) {
        String beanName = "plugins." + root.toLowerCase(Locale.ROOT);
        if (!ctx.containsBean(beanName)) {
            throw new CommandNotFoundException("[core] unmatched root command: " + beanName, "未匹配到命令模块: " + beanName);
        }
        return ctx.getBean(beanName);
    }

    /**
     * 结合命令优先级，返回优先级最高的方法
     * @param bean
     * @param sub
     * @return
     */
    private Method resolveMethod(Object bean, String sub) {
        Method[] all = bean.getClass().getMethods();
        List<Method> candidates = new ArrayList<>();
        for (Method m : all) {
            if (!m.getName().equalsIgnoreCase(sub)) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 2 &&
                    ((ps[0] == CommandChainContext.class && List.class.isAssignableFrom(ps[1])) ||
                            (ps[0] == com.arth.bot.core.common.dto.ParsedPayloadDTO.class && List.class.isAssignableFrom(ps[1])))) {
                candidates.add(m);
            } else if (ps.length == 1 &&
                    (ps[0] == CommandChainContext.class || ps[0] == com.arth.bot.core.common.dto.ParsedPayloadDTO.class
                            || List.class.isAssignableFrom(ps[0]))) {
                candidates.add(m);
            } else if (ps.length == 0) {
                candidates.add(m);
            }
        }
        if (candidates.isEmpty()) {
            throw new CommandNotFoundException("[core] command not supported: " + sub, "不支持的命令: " + sub);
        }
        candidates.sort(Comparator.comparingInt(CommandInvoker::scoreSignature).reversed());
        return candidates.get(0);
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
                else if (ps[0] == com.arth.bot.core.common.dto.ParsedPayloadDTO.class)
                    return m.invoke(bean, chainCtx.getPayload());
                else return m.invoke(bean, args);
            } else {
                return m.invoke(bean);
            }
        } catch (IllegalAccessException e) {
            log.error("[core] unable to invoke method: " + m.getName(), e);
            throw new InternalServerErrorException("Internal Server Error: " + e.getMessage(), "服务器内部错误；非法的反射调用");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException) {
                throw (BusinessException) cause;
            }
            log.error("[core] unable to invoke method: " + m.getName(), e);
            throw new InternalServerErrorException("Internal Server Error: " + e.getMessage(), "服务器内部错误：未知的反射执行时异常");
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