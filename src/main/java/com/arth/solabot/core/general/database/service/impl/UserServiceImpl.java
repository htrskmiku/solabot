package com.arth.solabot.core.general.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arth.solabot.core.general.database.domain.User;
import com.arth.solabot.core.general.database.service.UserService;
import com.arth.solabot.core.general.database.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author asheo
* @description 针对表【t_user】的数据库操作Service实现
* @createDate 2025-10-23 02:49:32
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




