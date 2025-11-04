package com.arth.solabot.core.bot.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Resource Not Found", null);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, null);
    }

    public ResourceNotFoundException(String message, String description) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, description);
    }
}