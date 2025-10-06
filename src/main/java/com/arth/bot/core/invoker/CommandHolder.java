package com.arth.bot.core.invoker;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 封装插件 Bean 与命令别名映射
 */
public class CommandHolder {

    public final Object bean;
    public final Map<String, List<Method>> commandRegistry;

    public CommandHolder(Object bean) {
        this.bean = bean;
        commandRegistry = new ConcurrentHashMap<>();
    }
}
