package com.arth.bot.core.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arth.bot.core.database.domain.StreamerSubscription;
import com.arth.bot.core.database.service.StreamerSubscriptionService;
import com.arth.bot.core.database.mapper.StreamerSubscriptionMapper;
import org.springframework.stereotype.Service;

/**
* @author asheo
* @description 针对表【t_streamer_subscription】的数据库操作Service实现
* @createDate 2025-10-23 02:49:32
*/
@Service
public class StreamerSubscriptionServiceImpl extends ServiceImpl<StreamerSubscriptionMapper, StreamerSubscription>
    implements StreamerSubscriptionService{

}




