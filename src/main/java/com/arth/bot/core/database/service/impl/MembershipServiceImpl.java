package com.arth.bot.core.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arth.bot.core.database.domain.Membership;
import com.arth.bot.core.database.service.MembershipService;
import com.arth.bot.core.database.mapper.MembershipMapper;
import org.springframework.stereotype.Service;

/**
* @author asheo
* @description 针对表【t_membership】的数据库操作Service实现
* @createDate 2025-10-23 02:49:32
*/
@Service
public class MembershipServiceImpl extends ServiceImpl<MembershipMapper, Membership>
    implements MembershipService{

}




