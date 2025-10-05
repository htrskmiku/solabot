package com.arth.bot.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.arth.bot.core.domain.Group;
import com.arth.bot.core.service.GroupService;
import com.arth.bot.core.mapper.GroupMapper;
import org.springframework.stereotype.Service;

/**
* @author asheo
* @description 针对表【t_group】的数据库操作Service实现
* @createDate 2025-10-06 01:09:29
*/
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group>
    implements GroupService{

}




