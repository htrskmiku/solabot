package com.arth.bot.core.common.dto.message;

import lombok.Data;

import java.util.Map;

@Data
public class MfaceSegment implements MessageSegment {

    private final String type = "mface";

    private Map<String, String> data;
}
