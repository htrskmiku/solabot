package com.arth.solabot.core.bot.dto.replay;

import lombok.Data;

@Data
public final class VideoRef {

    private MediaSourceType sourceType;

    private String source;

    private String mime;

    private Integer durationSec;

    private String filename;
}