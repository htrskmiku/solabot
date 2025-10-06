package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.domain.PjskBinding;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.core.mapper.PjskBindingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component("plugins.pjsk")
@BotPlugin({"pjsk"})
@RequiredArgsConstructor
public class Pjsk {

    private final Sender sender;
    private final PjskBindingMapper pjskBindingMapper;

    @Value("${server.port}")
    private String port;

    @Value("${app.client-access-url}")
    private String clientAccessUrl;

    public void index(ParsedPayloadDTO payload) {
        sender.responseText(payload, "请接 pjsk 模块的具体命令哦");
    }

    @BotCommand({"绑定"})
    public void bind(ParsedPayloadDTO payload, List<String> args) {
        if (args.isEmpty()) {
            bound(payload);
            return;
        }

        long qqId = payload.getUserId();
        Long groupId = payload.getGroupId();
        String pjskId = args.get(0);

        // 1. 查询
        LambdaQueryWrapper<PjskBinding> queryWrapper = new LambdaQueryWrapper<PjskBinding>()
                .eq(PjskBinding::getPjskId, pjskId)
                .eq(PjskBinding::getQqNumber, qqId)
                .eq(PjskBinding::getGroupId, groupId);

        PjskBinding binding = pjskBindingMapper.selectOne(queryWrapper);

        // 2. 插入或更新
        if (binding == null) {
            // 不存在记录，插入
            PjskBinding newBinding = new PjskBinding();
            newBinding.setPjskId(pjskId);
            newBinding.setQqNumber(qqId);
            newBinding.setGroupId(groupId);
            newBinding.setServerRegion("xx");
            newBinding.setCreatedAt(new Date());
            newBinding.setUpdatedAt(new Date());
            pjskBindingMapper.insert(newBinding);
            sender.sendText(payload, "bind successfully!");
        } else {
            // 存在记录，更新
            binding.setServerRegion("xx");
            binding.setUpdatedAt(new Date());
            pjskBindingMapper.updateById(binding);
            sender.sendText(payload, "pjsk binding just updated");
        }
    }

    @BotCommand({"查询绑定"})
    public void bound(ParsedPayloadDTO payload) {
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

    public void msm(ParsedPayloadDTO payload) {
        long qqId = payload.getUserId();
        LambdaQueryWrapper<PjskBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PjskBinding::getQqNumber, qqId);
        PjskBinding binding = pjskBindingMapper.selectOne(queryWrapper);

        if (binding == null) {
            sender.responseText(payload, "数据库中没有查询到你绑定的 pjsk 账号哦");
            return;
        }

        String pjskId = binding.getPjskId();
        String overviewImgUrl = "http://" + clientAccessUrl + ":" + port + "/resource/cn/mysekai/" + pjskId + "/overview";
        String mapImgUrl = "http://" + clientAccessUrl + ":" + port + "/resource/cn/mysekai/" + pjskId + "/map";
        sender.responseImage(payload, List.of(overviewImgUrl, mapImgUrl));
    }
}