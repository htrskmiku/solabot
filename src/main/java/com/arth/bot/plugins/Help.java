package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("plugins.help")
@BotPlugin({"help"})
@RequiredArgsConstructor
public class Help {

    private final Sender sender;

    public void index(ParsedPayloadDTO payload) {
        sender.responseText(payload, """
                这是 solabot，目前支持以下命令：
                
                - pjsk:
                    - 绑定 xxx: 绑定 pjsk 账号
                    - 查询绑定: 查看 pjsk 账号的绑定
                    
                - test:
                    - quanxian: 测试鉴权切面，硬编码仅允许 1093664084
                    - zuse <time>: 测试多线程异步，支持输入的时间参数 <time>
                    - huifu: 测试 bot 回复
                    - tu: 测试发图
                    - shipin: 测试发视频
                    - zhuanfa <QQid> <QQname> <text>: 测试链式构造合并转发消息，可以伪造消息记录
                    - yinyong <args...>: 测试 bot 获取图片引用消息
                """);
    }
}