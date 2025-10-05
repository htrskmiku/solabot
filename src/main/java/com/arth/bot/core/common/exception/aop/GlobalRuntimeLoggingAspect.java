package com.arth.bot.core.common.exception.aop;

import com.arth.bot.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RuntimeException 的兜底异常拦截器，一般不应由 bot 向用户回复
 */
@Slf4j
@Aspect
@Component
@Order(9999)
public class GlobalRuntimeLoggingAspect {
    @AfterThrowing(
            pointcut = "execution(* com.arth.bot..*(..))",
            throwing = "e"
    )
    public void logUnhandled(RuntimeException e) {
        if (e instanceof BusinessException) return;

        log.error("Unhandled runtime exception", e);
    }
}
