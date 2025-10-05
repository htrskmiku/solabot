package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.InvalidCommandArgsException;
import com.arth.bot.core.domain.PjskBinding;
import com.arth.bot.core.mapper.PjskBindingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component("plugins.pjsk")
@RequiredArgsConstructor
public class Pjsk {

    private final Sender sender;
    private final PjskBindingMapper pjskBindingMapper;

    public void index(ParsedPayloadDTO payload) {
    }

    public void bind(ParsedPayloadDTO payload, List<String> args) {
        if (args.isEmpty()) throw new InvalidCommandArgsException("lack of argument");

        long qqId = payload.getUserId();
        Long groupId = payload.getGroupId();
        String pjskId = args.get(0);

        PjskBinding binding = new PjskBinding();
        binding.setPjskId(pjskId);
        binding.setQqNumber(qqId);
        binding.setGroupId(groupId);

        binding.setCreatedAt(new Date());
        binding.setUpdatedAt(new Date());
        pjskBindingMapper.insert(binding);

        sender.sendText(payload, "bind successfully!");
    }

    public void check(ParsedPayloadDTO payload) {
        long qqId = payload.getUserId();
        LambdaQueryWrapper<PjskBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PjskBinding::getQqNumber, qqId);
        PjskBinding binding = pjskBindingMapper.selectOne(queryWrapper);
        if (binding == null) {
            sender.responseText(payload, "you haven't bound any pjsk id yet");
        } else {
            sender.responseText(payload, "your pjsk id is " + binding.getPjskId());
        }
    }
}