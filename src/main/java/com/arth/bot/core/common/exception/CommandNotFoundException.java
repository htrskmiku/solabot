package com.arth.bot.core.common.exception;

public class CommandNotFoundException extends BusinessException {

    public CommandNotFoundException() {
        super(ErrorCode.COMMAND_NOT_FOUND, "Command Not Found", null);
    }

    public CommandNotFoundException(String message) {
        super(ErrorCode.COMMAND_NOT_FOUND, message, null);
    }

    public CommandNotFoundException(String message, String description) {
        super(ErrorCode.COMMAND_NOT_FOUND, message, description);
    }
}
