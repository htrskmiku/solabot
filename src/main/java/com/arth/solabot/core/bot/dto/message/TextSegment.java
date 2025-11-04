package com.arth.solabot.core.bot.dto.message;

import lombok.Data;

import java.util.Map;

@Data
public class TextSegment implements MessageSegment {

    private final String type = "text";

    private Map<String, String> data;
}