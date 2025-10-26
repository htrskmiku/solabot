package com.arth.bot.core.invoker;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;

import java.util.List;

/**
 * 统一的命令处理器抽象。
 * - 直接以函数式封装插件方法（通过 MethodHandle 绑定）
 * - 兼容链式上下文（CommandChainContext）
 * - 返回 Object：用于老逻辑“将返回值写入 ctx.state”
 */
@FunctionalInterface
interface CommandHandler {

    /**
     * 执行该命令处理器
     *
     * @param chainCtx 链式上下文（可为 null，但调用端会始终传入）
     * @param payload  消息负载
     * @param args     文本参数列表
     * @return 返回值（若非 null，将被写入 chainCtx.setState(...)）
     * @throws Throwable 插件方法的任何异常直接透传，由 Invoker 统一转译
     */
    Object handle(CommandChainContext chainCtx, ParsedPayloadDTO payload, List<String> args) throws Throwable;

    /**
     * 打分（默认实现：0），PluginRegistry.createHandler 将重写此方法
     * (ctx,args)=6 > (ctx)=5 > (payload,args)=4 > (payload)=3 > (args)=2 > ()=1
     */
    default int score(ParsedPayloadDTO payload, List<String> args) {
        return 0;
    }

    /**
     * 用于 “分步分组” 时判断当前处理器是否 “接收 List 参数”，从而决定是否吞掉后续别名 token；
     * PluginRegistry.createHandler 将按签名覆写该行为。
     */
    default boolean acceptsArgs() {
        return false;
    }
}