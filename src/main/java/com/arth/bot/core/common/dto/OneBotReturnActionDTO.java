package com.arth.bot.core.common.dto;

import lombok.Data;

import java.util.Map;

/** 遵从 OneBot v11 定义的 action JSON POJO 对象 */
@Data
public class OneBotReturnActionDTO {

    private String action;

    private Map<String, Object> params;

    private String echo;
}
