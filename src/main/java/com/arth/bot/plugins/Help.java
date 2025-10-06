package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ForwardChainBuilder;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("plugins.help")
@BotPlugin({"help"})
@RequiredArgsConstructor
public class Help {

    private final Sender sender;
    private final ForwardChainBuilder forwardChainBuilder;

    public void index(ParsedPayloadDTO payload) {
        String json = forwardChainBuilder.create()
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        这是 solabot，正宗的纯血 java bot，目前支持以下三个模块：
                          1. pjsk 啤酒烧烤
                          2. img 图片处理
                          3. test 测试"""))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        pjsk 啤酒烧烤模块目前支持以下命令：
                          - 绑定 xxx: 绑定 pjsk 账号
                          - 查询绑定: 查看 pjsk 账号的绑定
                          - msm: 查看绑定的 mysekai"""))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        我们的绑定功能没有接游戏 api，目前唯一的作用是定位自己的 mysekai，所以输错了也不会有提示"""))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        👇要使用 mysekai 功能，iOS 请将使用下面的模块配置，以国服为例（其实目前也只硬编码了国服，其他服建议用 hrk 的）："""))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        #!name=国服烤森远程转发
                        #!desc=抓取游戏数据并转发到远程服务器
                        #!author=魔改自NeuraXmy
                        #!mitm=2
                        #!total=3
                        
                        [URL Rewrite]
                        ^https:\\/\\/submit\\.backtr        ace\\.io\\/  reject
                        
                        [Script]
                        SCRIPT_upload = type=http-response, requires-body=1, binary-body-mode=1, max-size=100000000, timeout=60, pattern=^https:\\/\\/mkcn-prod-public-60001-1\\.dailygn\\.com\\/api\\/user\\/(\\d+)\\/mysekai\\?isForceAllReloadOnlyMysekai\\=(True|False)$, script-path=https://yly.dylancloud.uk/upload.js
                        
                        [Mitm]
                        hostname=%APPEND% mkcn-prod-public-60001-1.dailygn.com, submit.backtrace.io"""))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        img 图片处理模块目前支持以下命令：
                        还没写好"""))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        test 测试模块目前支持以下命令：
                          - quanxian: 测试鉴权切面，硬编码仅允许 1093664084
                          - zuse <delay_time>: 测试多线程异步
                          - huifu: 测试 bot 回复
                          - tu: 测试发图
                          - shipin: 测试发视频
                          - zhuanfa <QQid> <QQname> <text>: 测试链式构造合并转发消息
                          - yinyong <args...>: 测试 bot 获取图片引用消息"""))
                .toGroupJson(payload.getGroupId());

        sender.pushActionJSON(payload.getSelfId(), json);
    }
}