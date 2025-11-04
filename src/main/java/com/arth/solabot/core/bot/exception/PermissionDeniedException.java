package com.arth.solabot.core.bot.exception;

public class PermissionDeniedException extends BusinessException {

    public PermissionDeniedException() {
        super(ErrorCode.PERMISSION_DENIED, "Permission Denied", null);
    }

    public PermissionDeniedException(String message) {
        super(ErrorCode.PERMISSION_DENIED, message, null);
    }

    public PermissionDeniedException(String message, String description) {
        super(ErrorCode.PERMISSION_DENIED, message, description);
    }
}
