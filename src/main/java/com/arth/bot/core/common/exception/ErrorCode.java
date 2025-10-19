package com.arth.bot.core.common.exception;

public enum ErrorCode {

    PERMISSION_DENIED,

    INVALID_COMMAND_ARGS,

    COMMAND_NOT_FOUND,

    RESOURCE_NOT_FOUND,

    INTERNAL_SERVER_ERROR,

    EXTERNAL_SERVICE_ERROR;

    @Override
    public String toString() {
        return this.name().toLowerCase().replace('_', ' ');
    }
}
