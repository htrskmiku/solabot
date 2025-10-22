package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.sender.action.ActionChainBuilder;
import com.arth.bot.core.authorization.annotation.DirectAuthInterceptor;
import com.arth.bot.core.authorization.model.AuthMode;
import com.arth.bot.core.authorization.model.AuthScope;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.database.domain.PjskBinding;
import com.arth.bot.core.invoker.annotation.BotCommand;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import com.arth.bot.core.database.mapper.PjskBindingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Slf4j
@BotPlugin({"pjsk"})
@RequiredArgsConstructor
public class Pjsk extends Plugin {

    private final Sender sender;
    private final ActionChainBuilder actionChainBuilder;
    private final PjskBindingMapper pjskBindingMapper;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Value("${server.port}")
    private String port;

    @Value("${app.client-access-url}")
    private String clientAccessUrl;

    private String baseUrl;

    @Getter
    public final String helpText = "请通过 /help 查看 pjsk 模块具体的命令";

    @PostConstruct
    public void init() {
        this.baseUrl = "http://" + clientAccessUrl + ":" + port;
    }

    @BotCommand("index")
    @Override
    public void index(ParsedPayloadDTO payload) {
        sender.replyText(payload, "请接 pjsk 模块的具体命令哦");
    }

    @BotCommand("help")
    @Override
    public void help(ParsedPayloadDTO payload) {
        sender.replyText(payload, helpText);
    }

    @BotCommand({"绑定", "bind"})
    public void bind(ParsedPayloadDTO payload, List<String> args) {
        if (args.isEmpty()) {
            bound(payload);
            return;
        }

        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        String pjskId = args.get(0);

        // 1. 查询
        PjskBinding binding = pjskBindingMapper.selectOne(queryBinding(userId, groupId));

        // 2. 插入或更新
        if (binding == null) {
            // 不存在记录，插入
            PjskBinding newBinding = new PjskBinding();
            newBinding.setPjskId(pjskId);
            newBinding.setUserId(userId);
            newBinding.setGroupId(groupId);
            newBinding.setServerRegion("xx");
            newBinding.setCreatedAt(new Date());
            newBinding.setUpdatedAt(new Date());
            pjskBindingMapper.insert(newBinding);
            sender.sendText(payload, "bind successfully!");
        } else {
            // 存在记录，更新
            binding.setPjskId(pjskId);
            binding.setServerRegion("xx");
            binding.setUpdatedAt(new Date());
            pjskBindingMapper.updateById(binding);
            sender.sendText(payload, "pjsk binding just updated");
        }
    }

    @BotCommand({"查询绑定", "bound"})
    public void bound(ParsedPayloadDTO payload) {
        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        PjskBinding binding = pjskBindingMapper.selectOne(queryBinding(userId, groupId));
        if (binding == null) {
            sender.replyText(payload, "you haven't bound any pjsk id yet");
        } else {
            sender.replyText(payload, "your pjsk id is " + binding.getPjskId());
        }
    }

    @BotCommand({"msm", "msr"})
    public void msm(ParsedPayloadDTO payload) {
        long userId = payload.getUserId();
        Long groupId = payload.getGroupId();
        PjskBinding binding = pjskBindingMapper.selectOne(queryBinding(userId, groupId));

        if (binding == null) {
            sender.replyText(payload, "数据库中没有查询到你绑定的 pjsk 账号哦");
            return;
        }

        String pjskId = binding.getPjskId();
        String region = binding.getServerRegion();

        // Path file = Path.of(System.getProperty("user.dir") + "/dynamic/pjsk_user_data/mysekai/draw/map/" + "cn" + "_" + pjskId + ".png");
        Path file = findPjskFileTMP(pjskId);
        String updatedTime = null;

        if (!Files.exists(file)) {
            sender.replyText(payload, "服务器上没有找到你的 MySekai 数据，可能是抓包未成功，小概率服务器解析失败，需要根据日志分析");
            return;
        } else {
            try {
                FileTime timestamp = Files.readAttributes(file, BasicFileAttributes.class).lastModifiedTime();
                updatedTime = dateTimeFormatter.format(timestamp.toInstant());
            } catch (IOException e) {
                sender.replyText(payload, "MySekai 数据存在，但获取更新日期失败: 抛出了 IOException");
                throw new InternalServerErrorException("IOException: " + e.getCause().getMessage(), "MySekai 数据存在，但获取更新日期失败: IOException");
            }
        }

        region = file.getFileName().toString().substring(0, 2);
        String overviewImgUrl = baseUrl + "/pjsk/resource/" + region + "/mysekai/" + pjskId + "/overview";
        String mapImgUrl = baseUrl + "/pjsk/resource/" + region + "/mysekai/" + pjskId + "/map";

        ActionChainBuilder builder = actionChainBuilder.create().setReplay(payload.getMessageId())
                .text("MySekai 数据更新于" + updatedTime)
                .image(overviewImgUrl)
                .image(mapImgUrl);

        String json = payload.getMessageType().equals("group") ?
                builder.toGroupJson(payload.getGroupId()) :
                builder.toPrivateJson(payload.getUserId());

        sender.pushActionJSON(payload.getSelfId(), json);
    }

    /**
     * 临时性方法，能跑就行 TMP
     * @param pjskId
     * @return
     */
    @Deprecated
    private Path findPjskFileTMP(String pjskId) {
        String dirPath = System.getProperty("user.dir") + "/dynamic/pjsk_user_data/mysekai/draw/map/";
        Path directory = Path.of(dirPath);
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return Path.of(dirPath, "default.png");
        }
        try {
            return Files.list(directory)
                    .filter(file -> {
                        String fileName = file.getFileName().toString();
                        return fileName.matches("[a-z]{2}_" + pjskId + "\\.png");
                    })
                    .findFirst()
                    .orElse(Path.of(dirPath, "default.png"));
        } catch (IOException e) {
            log.error("没有找到指定的数据", e);
            return Path.of(dirPath, "default.png");
        }
    }

    @BotCommand({"初始化", "initUserBinding", "init"})
    @DirectAuthInterceptor(scope = AuthScope.USER, mode  = AuthMode.ALLOW, targets = "1093664084")
    public void initUserBinding(ParsedPayloadDTO payload) {
        Object[][] qunYouId = {
                {1256977415L, "7485938033569569588"},  // 小萌
                {1685280357L, "7445096955522390818"},  // pl
                {1828209434L, "7487212719486049063"},  // 小弦
                {984097301L, "7486314772426939173"},   // 日蚀
                {1461762986L, "7489244575534537481"},  // 热可可咖啡
                {1093664084L, "123"}  // test
        };

        for (Object[] pair : qunYouId) {
            PjskBinding a = new PjskBinding();
            a.setPjskId((String) pair[1]);
            a.setUserId((Long) pair[0]);
            a.setGroupId(793709714L);
            a.setServerRegion("xx");
            a.setCreatedAt(new Date());
            a.setUpdatedAt(new Date());
            try {
                pjskBindingMapper.insert(a);
            } catch (Exception ignored) {

            }
        }

        sender.replyText(payload, "database init successfully");
    }

    private LambdaQueryWrapper<PjskBinding> queryBinding(long userId, Long groupId) {
        LambdaQueryWrapper<PjskBinding> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PjskBinding::getUserId, userId).eq(PjskBinding::getServerRegion, "xx");
        if (groupId == null) {
            queryWrapper.isNull(PjskBinding::getGroupId);
        } else {
            queryWrapper.eq(PjskBinding::getGroupId, groupId);
        }
        return queryWrapper;
    }
}