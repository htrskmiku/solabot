package com.arth.solabot.core.bot.dto.replay;

import lombok.Data;

@Data
public final class ImageRef {

    private MediaSourceType sourceType;     // URL/FILE_ID/PATH

    private String source;                  // 具体值（链接/文件id/本地路径）

    private String mime;                    // image/jpeg 等

    private String filename;

    private Integer width;

    private Integer height;

    private String md5;
}