package com.arth.bot.adapter.fetcher;

import com.arth.bot.core.common.dto.ReplayedMessagePayloadDTO;

public interface ReplyFetcher {

    /**
     * 获取当前消息所引用的消息
     *
     * @param selfId
     * @param messageId
     * @return
     */
    ReplayedMessagePayloadDTO fetch(long selfId, long messageId);
}