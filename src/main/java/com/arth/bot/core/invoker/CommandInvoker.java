package com.arth.bot.core.invoker;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.BusinessException;
import com.arth.bot.core.common.exception.CommandNotFoundException;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.plugins.DefaultStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandInvoker {

    private final PluginRegistry pluginRegistry;
    private final DefaultStrategy defaultStrategy = new DefaultStrategy();

    public Object invokeByPayload(ParsedPayloadDTO payload) {
        try {
            String line = payload.getCommandText();
            if (line == null || line.isBlank()) {
                if ("message".equals(payload.getPostType())) {
                    defaultStrategy.defaultHandle(payload);
                }
                return null;
            }
            String lline = line.stripLeading();
            if (lline.isEmpty() || lline.charAt(0) != '/') {
                defaultStrategy.defaultHandle(payload);
                return null;
            }
            Parsed parsed = parse(lline);
            if (parsed == null) return null;
            String parsedRoot = parsed.root();
            log.debug("[core.invoker] command detected: {}", parsedRoot);
            PluginHolder holder = resolvePluginHolder(parsedRoot);
            List<Step> steps = groupSteps(parsed.subAndArgs(), holder);
            if (steps.isEmpty()) {
                // 无明确子命令时，默认执行 index
                steps = List.of(new Step(indexAlias(), List.of()));
            }
            // 链式上下文贯穿，贪心匹配命令
            CommandChainContext chainCtx = new CommandChainContext(payload);
            Object last = null;
            for (Step s : steps) {
                // 贪心匹配候选命令
                List<CommandHandler> candidates = getCandidatesWithIndexFallback(holder, s.name());
                if (candidates.isEmpty()) {
                    throw new CommandNotFoundException(
                            "[core.invoker] command not supported: " + s.name(),
                            "不支持的命令: " + s.name());
                }
                // 命中候选命令
                CommandHandler preferred = holder.getPreferred(s.name());
                if (preferred == null) {
                    preferred = chooseBest(candidates, payload, s.args());
                    holder.setPreferred(s.name(), preferred);
                }
                // 返回值注入链上下文
                Object ret = preferred.handle(chainCtx, payload, s.args());
                if (ret != null) chainCtx.setState(ret);
                last = ret;
            }
            return last;
        } catch (BusinessException e) {
            throw e;
        } catch (Throwable e) {
            log.error("[core.invoker] unexpected error while invoking", e);
            throw new InternalServerErrorException(
                    "Internal Server Error: " + e.getMessage(),
                    "服务器内部错误：未知异常");
        }
    }

    /**
     * 解析命令字符串，返回 Parsed 对象（根命令（首个 token）与 tokens，tokens 包含子命令与参数）
     *
     * @param raw
     * @return
     */
    private Parsed parse(String raw) {
        String s = raw.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("^/+", "/");
        String cmd = s.substring(1).trim();
        List<String> tokens = new ArrayList<>(Arrays.asList(cmd.split(" ")));
        if (tokens.isEmpty()) return null;
        String root = tokens.remove(0).toLowerCase(Locale.ROOT);
        return new Parsed(root, tokens);
    }

    /**
     * 进一步解析命令字符串，将 tokens 解析为子命令与对应的参数
     * 规则：当遇到一个 token 在 methodNames 中时，判定当前正在收集参数的命令是否允许接受参数；
     * 如果允许，则把该 token 当作参数；
     * 否则将其视为新的命令名。
     *
     * @param tokens
     * @param holder
     * @return
     */
    private List<Step> groupSteps(List<String> tokens, PluginHolder holder) {
        List<Step> out = new ArrayList<>();
        if (tokens == null || tokens.isEmpty()) return out;
        // 仅当首个 token 不是子命令别名时，才启动 index 作为当前命令
        String curName = null;
        List<String> curArgs = new ArrayList<>();
        // 子命令别名全集（小写）
        Set<String> methodNames = holder.aliases();
        for (int idx = 0; idx < tokens.size(); idx++) {
            String tk = tokens.get(idx);
            String low = tk.toLowerCase(Locale.ROOT);
            if (methodNames.contains(low)) {
                // 命令起始或可能的“参数/新命令”分界
                if (curName == null) {
                    // 首次遇到子命令别名：直接将其视为当前命令名，避免生成空的 index 步
                    curName = low;
                    curArgs.clear();
                    continue;
                }
                // 分组阶段的贪心策略：只要当前命令存在“任一接参实现”，就把该 token 吞为参数
                boolean acceptsList = acceptsArgsForGrouping(holder, curName);
                if (acceptsList) {
                    curArgs.add(tk);
                } else {
                    // 当前命令不接参，结算并切换到新命令
                    out.add(new Step(curName, List.copyOf(curArgs)));
                    curName = low;
                    curArgs = new ArrayList<>();
                }
            } else {
                // 普通参数
                if (curName == null) {
                    // 首个 token 不是子命令别名时，启动 index 作为当前命令来承接参数
                    curName = indexAlias();
                }
                curArgs.add(tk);
            }
        }
        if (curName != null) {
            out.add(new Step(curName, List.copyOf(curArgs)));
        }
        return out;
    }

    /**
     * 用于分组阶段的 “临时优选解析”：优先使用缓存；若无缓存则使用已有的 args 对候选打分，计算一个 “是否接收 List” 的代表处理器
     *
     * @param holder
     * @param aliasLower
     * @param args
     * @return
     */
    private CommandHandler resolvePreferredForGrouping(PluginHolder holder, String aliasLower, List<String> args) {
        CommandHandler preferred = holder.getPreferred(aliasLower);
        if (preferred != null) return preferred;
        List<CommandHandler> candidates = getCandidatesWithIndexFallback(holder, aliasLower);
        if (candidates == null || candidates.isEmpty()) {
            throw new CommandNotFoundException("[core.invoker] command not supported: " + aliasLower, "不支持的命令: " + aliasLower);
        }
        CommandHandler best = chooseBest(candidates, null, args);
        holder.setPreferred(aliasLower, best);
        return best;
    }

    private List<CommandHandler> getCandidatesWithIndexFallback(PluginHolder holder, String aliasLower) {
        Map<String, List<CommandHandler>> all = holder.getAllHandlers();
        List<CommandHandler> list = all.get(aliasLower);
        if (list != null && !list.isEmpty()) return list;
        list = all.get(indexAlias());  // 这里 "" 视为 index
        if (list != null && !list.isEmpty()) return list;
        list = all.get("index");
        if (list != null && !list.isEmpty()) return list;
        return List.of();
    }

    /**
     * 评分择优：ctx+args > ctx > payload+args > payload > args > none
     *
     * @param cands
     * @param payload
     * @param args
     * @return
     */
    private CommandHandler chooseBest(List<CommandHandler> cands, ParsedPayloadDTO payload, List<String> args) {
        // 运行期择优：基于 “是否带参” 对接参/不接参的处理器进行轻/重权重偏置
        boolean hasArgs = args != null && !args.isEmpty();
        int best = Integer.MIN_VALUE;
        CommandHandler bestH = null;
        for (CommandHandler h : cands) {
            int s = h.score(payload, args);
            if (hasArgs && !h.acceptsArgs()) {
                s -= 1000; // 带参但处理器不接参，狠狠惩罚
            } else if (!hasArgs && h.acceptsArgs()) {
                s -= 100;   // 无参但处理器接参，惩罚
            } else {
                s += 10;   // 匹配期望，加分
            }
            if (s > best) {
                best = s;
                bestH = h;
            }
        }
        return bestH != null ? bestH : cands.get(0);
    }

    private String indexAlias() {
        return "";
    }

    private PluginHolder resolvePluginHolder(String rootKey) {
        PluginHolder holder = pluginRegistry.getPluginHolder(rootKey);
        if (holder == null) {
            throw new CommandNotFoundException(
                    "[core.invoker] unmatched root command: " + rootKey,
                    "未匹配到命令模块: " + rootKey);
        }
        return holder;
    }

    /* ===================== data records ===================== */
    private record Parsed(String root, List<String> subAndArgs) {
    }

    private record Step(String name, List<String> args) {
    }

    /**
     * 分组阶段判断“是否接收参数”的专用逻辑：
     * 不做精细打分，只要存在任一接参实现，就认为当前命令可吞参（贪心参数），
     * 这样可避免在 args 仍为空时误选无参实现，导致把后续别名 token 当作“新命令”。
     */
    private boolean acceptsArgsForGrouping(PluginHolder holder, String aliasLower) {
        CommandHandler preferred = holder.getPreferred(aliasLower);
        if (preferred != null) return preferred.acceptsArgs();
        List<CommandHandler> candidates = getCandidatesWithIndexFallback(holder, aliasLower);
        if (candidates.isEmpty()) return false;
        for (CommandHandler h : candidates) {
            if (h.acceptsArgs()) return true;
        }
        return false;
    }
}

