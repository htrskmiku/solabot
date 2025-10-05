package com.arth.bot.core.common.dto.message;

import java.util.Map;

/** MessageSegment 有三个实现：AtSegment、ImageSegment 以及 TextSegment，均属于 ParsedPayloadDTO 的 segments 字段 */
public interface MessageSegment {

    String getType();

    Map<String, String> getData();
}