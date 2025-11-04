package com.arth.solabot.core.bot.exception;

public class InvalidCommandArgsException extends BusinessException {

    public InvalidCommandArgsException() {
        super(ErrorCode.INVALID_COMMAND_ARGS, "Invalid Command Args", null);
    }

    public InvalidCommandArgsException(String message) {
         super(ErrorCode.INVALID_COMMAND_ARGS, message, null);
    }

    public InvalidCommandArgsException(String message, String description) {
        super(ErrorCode.INVALID_COMMAND_ARGS, message, description);
    }
}
