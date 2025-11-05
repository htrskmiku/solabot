package com.arth.solabot.plugin.custom;

import com.arth.solabot.adapter.controller.ApiPaths;
import com.arth.solabot.adapter.sender.Sender;
import com.arth.solabot.core.bot.dto.ParsedPayloadDTO;
import com.arth.solabot.core.bot.invoker.annotation.BotCommand;
import com.arth.solabot.core.bot.invoker.annotation.BotPlugin;
import com.arth.solabot.plugin.resource.LocalData;
import com.arth.solabot.plugin.resource.MemoryData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 该插件依赖于外部程序 BaiduPCS-Go
 */
@Slf4j
@BotPlugin(value = {"看"}, glued = true)
@RequiredArgsConstructor
public class Gallery extends Plugin {

    @Value("${app.parameter.plugin.gallery.BaiduPCS-Go}")
    private String BAIDUPCS_GO;
    @Value("${app.parameter.plugin.gallery.luna-gallery-url}")
    private String UPDATE_REQUEST_URL;
    @Value("${app.parameter.plugin.gallery.pwd}")
    private String PWD;
    @Value("${app.parameter.plugin.gallery.update-ttl}")
    private int UPDATE_TTL;
    @Value("${app.parameter.plugin.gallery.update-cron}")
    private String UPDATE_CRON;

    private Instant lastUpdateTime = null;
    private Map<String, List<String>> roles = new HashMap<>();  // role → set(id)
    private List<String> ids = new ArrayList<>();
    private static Map<String, FilePair> idToFile = new HashMap<>();   // id → (role, filename)
    private final Random rand = new Random();

    private final Sender sender;
    private final ApiPaths apiPaths;

    @Getter
    public final String helpText = """
            看看你的模块现支持随机看表情包/梗图：
              - 所有烤 oc，例如 saki、mnr
              - 所有 v，例如 miku、kaito，包括 teto
              - 看猪，鸟，夜鹭，西巴，kiwi，企鹅
            
            以上是紧凑命令，无需空格，使用方法示例是：/看miku
            
            也可以按 pid 查看，例如：/看912。
            
            如果需要查看各角色的id-缩略图，在 “看” 后加上 “所有” 即可，例如：/看所有miku
            
            "/看看"、"/看看你的" 则会在全角色内随机
            
            非紧凑命令（需要空格）：
              - update: 强制与图源进行本地增量同步
            
            图库来自luna茶""";

    @BotCommand("index")
    public void index(ParsedPayloadDTO payload, List<String> args) {
        // tryUpdateGallery();
        updateRolesMapAndDrawThumbnails();

        for (String arg : args) {
            // 空字符
            if (arg == null || arg.isEmpty()) continue;

            // 全局随机
            if (arg.equals("看") || arg.equals("看你的")) {
                int idx = rand.nextInt(ids.size());
                String id = ids.get(idx);
                sender.sendImage(payload, apiPaths.buildGalleryImgUrl(id));
                continue;
            }

            // 缩略图
            if (arg.length() >= 2 && (arg.startsWith("所有") || arg.startsWith("全部"))) {
                sender.sendImage(payload, apiPaths.buildGalleryThumbnailUrl(arg.substring(2)));
                continue;
            }

            // 如果输入是纯数字，视为 pid
            if (arg.matches("\\d+")) {
                sender.sendImage(payload, apiPaths.buildGalleryImgUrl(arg));
                continue;
            }

            // 否则解释为别名
            String role = MemoryData.alias.get(arg);
            if (role != null) {
                List<String> idsForRole = roles.get(role);
                if (idsForRole != null) {
                    int idx = rand.nextInt(idsForRole.size());
                    String id = idsForRole.get(idx);
                    sender.sendImage(payload, apiPaths.buildGalleryImgUrl(id));
                }
            }
        }
    }

    @Override
    public void index(ParsedPayloadDTO payload) {

    }

    @BotCommand("help")
    public void help(ParsedPayloadDTO payload) {
        super.help(payload);
    }

    @BotCommand({"update", "刷新", "更新"})
    public void update(ParsedPayloadDTO payload) {
        updateAllGallery();
        sender.replyText(payload, "已同步至最新图库...");
    }


    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****


    public static FilePair getFilePairByPid(String pid) throws IOException {
        return idToFile.get(pid);
    }

    private void tryUpdateGallery() {
        if (lastUpdateTime == null || Duration.between(lastUpdateTime, Instant.now()).toHours() > UPDATE_TTL) {
            updateAllGallery();
        }
    }

    /**
     * 使用 <a href="https://github.com/qjfoidnh/BaiduPCS-Go">BaiduPCS-Go</a> 进行图库数据的同步下载
     */
    private void updateAllGallery() {
        log.info("[plugin.gallery] start syncing gallery...");

        synchronized (this) {
            String dirName = LocalData.GALLERY_DIR_NAME.getFileName().toString();
            Path localDir = LocalData.GALLERY_IMG_BASE;
            try {
                Files.createDirectories(localDir);
            } catch (IOException e) {
                log.error("[plugin.gallery] failed to create local gallery dir", e);
                return;
            }

            try {
                log.info("[pcs] transferring shared folder from {}", UPDATE_REQUEST_URL);
                ProcessBuilder importPb = new ProcessBuilder(
                        BAIDUPCS_GO, "transfer", UPDATE_REQUEST_URL, PWD
                );
                importPb.directory(new File("."));
                importPb.redirectErrorStream(true);
                Process importProcess = importPb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(importProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines().forEach(line -> log.info("[pcs] {}", line));
                }
                importProcess.waitFor(3, TimeUnit.MINUTES);
                int importCode = importProcess.exitValue();
                if (importCode != 0) {
                    log.warn("[pcs] transfer exited with code {}, continuing anyway", importCode);
                }

                log.info("[pcs] start downloading from /{} to {}", dirName, localDir);
                ProcessBuilder pb = new ProcessBuilder(
                        BAIDUPCS_GO, "download", "/" + dirName, "--ow", "--saveto", localDir.toString()
                );
                pb.directory(new File("."));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines().forEach(line -> log.info("[pcs] {}", line));
                }

                boolean finished = process.waitFor(10, TimeUnit.MINUTES);
                if (!finished) {
                    process.destroy();
                    log.warn("[plugin.gallery] pcs download timeout — process killed");
                } else {
                    int exitCode = process.exitValue();
                    if (exitCode == 0) {
                        updateRolesMapAndDrawThumbnails();
                        lastUpdateTime = Instant.now();
                        log.info("[plugin.gallery] sync completed successfully");
                    } else {
                        log.error("[plugin.gallery] pcs exited with code {}", exitCode);
                    }
                }

            } catch (Exception e) {
                log.error("[plugin.gallery] failed to sync gallery", e);
            }
        }
    }

    /**
     * 更新 Roles 映射表，同时绘制各角色缩略图
     */
    private void updateRolesMapAndDrawThumbnails() {
        Map<String, List<String>> newRoles = new HashMap<>((roles != null) ? roles.size() : 16);
        Map<String, FilePair> newIdToFile = new HashMap<>((idToFile != null) ? idToFile.size() : 16);

        try {
            Files.createDirectories(LocalData.GALLERY_THUMBNAILS_BASE);
        } catch (IOException e) {
            log.error("[plugin.gallery] failed to create thumbnail directory", e);
        }

        try (Stream<Path> roleDirs = Files.list(LocalData.GALLERY_IMG_DIR)) {
            roleDirs.filter(Files::isDirectory).forEach(roleDir -> {
                String roleName = roleDir.getFileName().toString();
                int mapSize = 16;
                if (roles != null) {
                    List<String> oldIdSet = roles.get(roleName);
                    if (oldIdSet != null) {
                        mapSize = oldIdSet.size();
                    }
                }
                Map<String, String> idToFileOfRole = new HashMap<>(mapSize);

                try (Stream<Path> files = Files.list(roleDir)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        String filename = file.getFileName().toString();
                        String id = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
                        idToFileOfRole.put(id, filename);
                        newIdToFile.put(id, new FilePair(roleName, filename));
                    });
                } catch (IOException e) {
                    log.error("[plugin.gallery] failed to list files for role: {}", roleName, e);
                }

                if (!idToFileOfRole.isEmpty()) {
                    newRoles.put(roleName, new ArrayList<>(idToFileOfRole.keySet()));

                    try {
                        int maxPerRow = 15;     // 每行最多 15 张图
                        int thumbWidth = 120;   // 缩略图最大宽度
                        int thumbHeight = 120;  // 缩略图最大高度
                        int labelHeight = 20;   // id标签高度
                        int margin = 10;        // 间距

                        int imageCount = idToFileOfRole.size();
                        int rowCount = (int) Math.ceil(imageCount / (double) maxPerRow);

                        int canvasWidth = maxPerRow * (thumbWidth + margin) + margin;
                        int canvasHeight = rowCount * (thumbHeight + labelHeight + margin) + margin;

                        BufferedImage gallery = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = gallery.createGraphics();
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, canvasWidth, canvasHeight);
                        g.setColor(Color.BLACK);
                        g.setFont(new Font("SansSerif", Font.PLAIN, 12));

                        AtomicInteger index = new AtomicInteger(0);
                        idToFileOfRole.forEach((id, filename) -> {
                            try {
                                Path file = roleDir.resolve(filename);
                                BufferedImage img = ImageIO.read(file.toFile());
                                if (img == null) return;  // skip if unreadable

                                int w = img.getWidth();
                                int h = img.getHeight();

                                double scale = Math.min((double) thumbWidth / w, (double) thumbHeight / h);
                                int scaledW = (int) (w * scale);
                                int scaledH = (int) (h * scale);

                                int xIndex = index.get() % maxPerRow;
                                int yIndex = index.get() / maxPerRow;
                                int x = margin + xIndex * (thumbWidth + margin);
                                int y = margin + yIndex * (thumbHeight + labelHeight + margin);

                                g.drawImage(img, x + (thumbWidth - scaledW) / 2, y + (thumbHeight - scaledH) / 2, scaledW, scaledH, null);
                                g.drawString(id, x + 5, y + thumbHeight + 15);
                                index.incrementAndGet();
                            } catch (Exception e) {
                                log.warn("[plugin.gallery] failed to process image for role {} id {}", roleName, id, e);
                            }
                        });

                        g.dispose();
                        Path outFile = LocalData.GALLERY_THUMBNAILS_BASE.resolve(roleName + ".png");
                        ImageIO.write(gallery, "png", outFile.toFile());
                        log.debug("[plugin.gallery] thumbnail generated: {}", outFile);

                    } catch (IOException e) {
                        log.error("[plugin.gallery] failed to generate thumbnail for role {}", roleName, e);
                    }
                }
            });
        } catch (IOException e) {
            log.error("[plugin.gallery] failed to update roles from local directory", e);
            return;
        }

        roles = newRoles;
        idToFile = newIdToFile;
        ids = new ArrayList<>(idToFile.keySet());
        log.info("[plugin.gallery] all roles updated and all thumbnails generated, found {} imgs", newIdToFile.size());
    }

    public record FilePair(String role, String filename) {
    }
}