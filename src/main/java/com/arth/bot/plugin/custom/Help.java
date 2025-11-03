package com.arth.bot.plugin.custom;

import com.arth.bot.adapter.controller.ApiPaths;
import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ForwardChainBuilder;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import lombok.RequiredArgsConstructor;

import java.util.List;

@BotPlugin({"help"})
@RequiredArgsConstructor
public class Help extends Plugin {

    private final Sender sender;
    private final ForwardChainBuilder forwardChainBuilder;
    private final ApiPaths apiPaths;

    @BotCommand("index")
    public void index(ParsedPayloadDTO payload) {
        ForwardChainBuilder building = forwardChainBuilder.create().addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        这里是 solabot，一只具有独立 java 后端的 bot，本世代为「ickk」，主要为翼遥烤群（某高校 pjsk 同好群）而设计，目前支持以下几个模块：
                          1. pjsk 啤酒烧烤
                          2. img 图片处理
                          3. 看看你的
                          4. live 直播订阅
                          5. test 测试
                        
                        命令的使用方法为 “/模块名 命令名 <参数>”，例如 /pjsk 绑定，注意有空格；
                        
                        可以通过 “/help 模块名” 或 “/模块名 help” 单独查看指定模块的帮助文档"""))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                        pjsk 啤酒烧烤模块目前支持以下命令：
                          - 绑定 <pjsk id> <可选 cn/tw/jp>: 绑定 pjsk 账号，默认国服
                          - 绑定 / 查询绑定: 查看 pjsk 账号的绑定
                          - 默认服务器 <cn/tw/jp>：切换默认服务器
                          - msm <可选 cn/tw/jp>: 查看所绑定的 mysekai 数据，默认国服
                          - box <-r> <可选 cn/tw/jp>: 查询 box，已实装，半成品，不加参数为按角色排序，-r参数为按稀有度降序排列，默认国服
                          - luna茶的组卡器，尚未实装"""));

        if (payload.getGroupId() == null || payload.getGroupId().equals(619096416L) || payload.getGroupId().equals(1036993047L) || payload.getGroupId().equals(570656202L) || payload.getGroupId().equals(992406250L) || payload.getGroupId().equals(916204609L)) {
            building.addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                            我们的绑定功能没有接游戏 api，目前唯一的作用是定位自己的 mysekai，所以输错了也不会有提示"""))
                    .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                            👇要使用 mysekai 功能，iOS 请将使用下面的模块配置，以国服+为例（需要其他服的模块可联系我）：在 配置→模块→右上角➕︎号，填入下面这个地址："""))
                    .addCustomNode(payload.getSelfId(), "bot", n -> n.text(apiPaths.getShadowrocketModuleDownloadMysekaiCn()))
                    .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                            模块的使用教程可以参考 https://bot.teaphenby.com/public/tutorial/tutorial.html，步骤大体相同，记得将模块替换为我们的"""));
        } else {
            building.addCustomNode(payload.getSelfId(), "bot", n -> n.text("「当前群聊非翼遥啤酒烧烤大排档，烤森功能不可用，pjsk 模块剩余内容略」"));
        }

        building.addCustomNode(payload.getSelfId(), "bot", n -> n.text(pluginRegistry.getPluginHelpText("Img")))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text(pluginRegistry.getPluginHelpText("看")))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text(pluginRegistry.getPluginHelpText("Live")))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text(pluginRegistry.getPluginHelpText("Test")));;

        String json = (payload.getGroupId() != null) ? building.toGroupJson(payload.getGroupId()) : building.toPrivateJson(payload.getUserId());

        sender.pushActionJSON(payload.getSelfId(), json);
    }

    @BotCommand("index")
    public void index(ParsedPayloadDTO payload, List<String> args) {
        for (String arg : args) {
            try {
                pluginRegistry.callPluginHelp(payload, arg);
            } catch (Exception ignore) {

            }
        }
    }

    @Override
    public String getHelpText() {
        return "";
    }
}