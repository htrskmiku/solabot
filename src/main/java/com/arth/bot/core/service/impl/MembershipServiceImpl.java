package com.arth.bot.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arth.bot.core.domain.Membership;
import com.arth.bot.core.service.MembershipService;
import com.arth.bot.core.mapper.MembershipMapper;
import org.springframework.stereotype.Service;

/**
* @author asheo
* @description 针对表【t_membership】的数据库操作Service实现
* @createDate 2025-10-06 01:09:29
*/
@Service
public class MembershipServiceImpl extends ServiceImpl<MembershipMapper, Membership>
    implements MembershipService{

}




