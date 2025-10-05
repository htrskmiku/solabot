package com.arth.bot.core.authorization.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DirectAuthInterceptors {
    DirectAuthInterceptor[] value();
}