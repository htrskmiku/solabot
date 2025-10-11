package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ForwardChainBuilder;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component("plugins.help")
@BotPlugin({"help"})
@RequiredArgsConstructor
public class Help {

    private final Sender sender;
    private final ForwardChainBuilder forwardChainBuilder;
    private final ApplicationContext applicationContext;

    public void index(ParsedPayloadDTO payload) {
        if (payload.getCommandText().matches("/help\\s+\\S+")) {
            pluginHelp(payload, payload.getCommandText().substring(6));
            return;
        }

        ForwardChainBuilder building = forwardChainBuilder.create().addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                这里是 solabot，一只具有独立 java 后端的 bot，本世代为「ickk」，主要为翼遥/风翼烤群而设计，目前支持以下三个模块：
                  1. pjsk 啤酒烧烤
                  2. img 图片处理
                  3. test 测试
                命令的使用方法为 “/模块名 命令名 <参数>”，示例：/pjsk 绑定；
                可以通过 “/help 模块名” 或 “/模块名 help” 单独查看指定模块的帮助文档"""))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                pjsk 啤酒烧烤模块目前支持以下命令：
                  - 绑定 <pjsk id>: 绑定 pjsk 账号
                  - 绑定 / 查询绑定: 查看 pjsk 账号的绑定
                  - msm: 查看所绑定的 mysekai 数据
                  - 初始化: 需要权限，初始化数据库"""));

        if (payload.getGroupId() == null || payload.getGroupId().equals(619096416L) || payload.getGroupId().equals(1036993047L) || payload.getGroupId().equals(570656202L)) {
            building.addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                    我们的绑定功能没有接游戏 api，目前唯一的作用是定位自己的 mysekai，所以输错了也不会有提示"""))
                    .addCustomNode(payload.getSelfId(), "bot", n -> n.text("""
                    👇要使用 mysekai 功能，iOS 请将使用下面的模块配置，以国服为例（其实目前也只硬编码了国服，其他服建议用 hrk 的，需要其他服的联系我）："""))
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
                    模块的使用教程可以参考 https://bot.teaphenby.com/public/tutorial/tutorial.html，步骤大体相同，记得将模块替换为我们的"""));
        } else {
            building.addCustomNode(payload.getSelfId(), "bot", n -> n.text("「当前群聊非翼遥/风翼啤酒烧烤大排档，烤森功能不可用」"));
        }

        building.addCustomNode(payload.getSelfId(), "bot", n -> n.text(Img.helpText))
                .addCustomNode(payload.getSelfId(), "bot", n -> n.text(Test.helpText));

        String json = (payload.getGroupId() != null) ? building.toGroupJson(payload.getGroupId()) : building.toPrivateJson(payload.getUserId());

        sender.pushActionJSON(payload.getSelfId(), json);
    }

    protected void pluginHelp(ParsedPayloadDTO payload, String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) {
            index(payload);
            return;
        }

        try {
            Object pluginBean = applicationContext.getBean("plugins." + pluginName);
            Class<?> clazz = pluginBean.getClass();
            Field field = clazz.getField("helpText");
            String helpTextStr = (String) field.get(null);
            ForwardChainBuilder building = forwardChainBuilder.create()
                    .addCustomNode(payload.getSelfId(), "bot", n -> n.text("下面是 " + pluginName + " 模块的帮助文本"))
                    .addCustomNode(payload.getSelfId(), "bot", n -> n.text(helpTextStr));
            String json = (payload.getGroupId() != null) ? building.toGroupJson(payload.getGroupId()) : building.toPrivateJson(payload.getUserId());
            sender.pushActionJSON(payload.getSelfId(), json);
        } catch (BeansException e) {
            sender.replyText(payload, "不存在指定 plugin 的 Bean 对象，是否输入了错误的 plugin 名称？");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            sender.replyText(payload, "尝试获取 plugin 的 Bean 对象帮助文档字段时抛出了反射异常");
        }
    }
}