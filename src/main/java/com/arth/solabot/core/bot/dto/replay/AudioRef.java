package com.arth.solabot.core.bot.dto.replay;

import lombok.Data;

@Data
public final class AudioRef {

    private MediaSourceType sourceType;     // URL/FILE_ID/PATH

    private String source;

    private String mime;                    // audio/amr 等

    private Integer durationSec;

    private String filename;
}