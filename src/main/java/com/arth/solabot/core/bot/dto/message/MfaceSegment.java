package com.arth.solabot.core.bot.dto.message;

import lombok.Data;

import java.util.Map;

@Data
public class MfaceSegment implements MessageSegment {

    private final String type = "mface";

    private Map<String, String> data;
}
