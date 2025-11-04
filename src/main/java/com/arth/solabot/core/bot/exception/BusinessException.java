package com.arth.solabot.core.bot.exception;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String description;  // bot 主动发送的可读错误信息（如有需要）

    public BusinessException(final ErrorCode errorCode, String message, String description) {
        super(message);
        this.errorCode = errorCode;
        this.description = description;
    }
}
