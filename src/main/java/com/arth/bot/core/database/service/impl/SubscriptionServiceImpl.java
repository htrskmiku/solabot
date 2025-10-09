package com.arth.bot.core.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arth.bot.core.database.domain.Subscription;
import com.arth.bot.core.database.service.SubscriptionService;
import com.arth.bot.core.database.mapper.SubscriptionMapper;
import org.springframework.stereotype.Service;

/**
* @author asheo
* @description 针对表【t_subscription】的数据库操作Service实现
* @createDate 2025-10-09 15:29:47
*/
@Service
public class SubscriptionServiceImpl extends ServiceImpl<SubscriptionMapper, Subscription>
    implements SubscriptionService{

}




