package com.arth.bot.core.authorization.annotation;

import com.arth.bot.core.authorization.model.AuthMode;
import com.arth.bot.core.authorization.model.AuthScope;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DirectAuthInterceptor {
    /** 直接鉴权对象：根据 id 字段直接判断，用户 or 群组，以用户级的判断优先 */
    AuthScope scope();

    /** 鉴权模式：白名单 or 黑名单 */
    AuthMode mode();

    /**
     * SpEL 表达式：返回规则名单，用于与目标 subjectId 匹配。
     *
     * 支持以下返回类型：
     * - 单个 Long/Number：如 "1001L" 或 "@conf.adminId()"
     * - 原生数组：long[] / int[] 等，如 "new long[]{1L,2L}"
     * - Iterable<? extends Number>：如 "@conf.adminUserIds()"（List<Long>）
     */
    String targets() default "";

    /** 错误信息 */
    String message() default "";
}