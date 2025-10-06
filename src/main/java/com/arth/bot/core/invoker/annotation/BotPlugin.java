package com.arth.bot.core.invoker.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotPlugin {
    /**
     * 模块别名注解
     * 例如，将 "/img" 与 "/图片" 映射到同一个插件模块
     */
    String[] value();
}
