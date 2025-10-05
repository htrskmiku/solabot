package com.arth.bot.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arth.bot.core.domain.User;
import com.arth.bot.core.service.UserService;
import com.arth.bot.core.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author asheo
* @description 针对表【t_user】的数据库操作Service实现
* @createDate 2025-10-06 01:09:29
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




