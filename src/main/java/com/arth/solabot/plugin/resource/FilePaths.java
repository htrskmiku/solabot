package com.arth.solabot.plugin.resource;

import com.arth.solabot.plugin.custom.pjsk.objects.PjskCard;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FilePaths {

    /* 服务器本地 Suite 路径 */
    public static final Path PJSK_SUITE_CN = Path.of("static/pjsk/master_data/suite/cn");
    public static final Path PJSK_SUITE_JP = Path.of("static/pjsk/master_data/suite/jp");
    public static final Path PJSK_SUITE_TW = Path.of("static/pjsk/master_data/suite/tw");
    public static final Path PJSK_SUITE_JSON_BASE = Path.of("static/pjsk/master_data/suite/");

    /* Mysekai 透视图 map 路径 */
    public static final Path PJSK_MYSEKAI_MAP = Path.of("dynamic/pjsk_user_data/mysekai/draw/map");
    /* Mysekai 透视图 overview 路径 */
    public static final Path PJSK_MYSEKAI_OVERVIEW = Path.of("dynamic/pjsk_user_data/mysekai/draw/overview");

    /* 日服 master data 数据路径 */
    public static final Path PJSK_MASTER_DATA_PATH = Path.of("static/pjsk/master_data");
    public static final Path PJSK_CARDS = Path.of("static/pjsk/master_data/cards");

    /* PJSK Shadowracket 国服 Mysekai 数据抓包转发模块配置文件路径 */
    public static final Path SHADOWROCKET_MODULE_DOWNLOAD_MYSEKAI_CN = Path.of("static/pjsk/proxy_software_module/shadowrocket/cn_mysekai.txt");

    /* PJSK 卡组渲染器相关资源路径 */
    public static final Path RENDER_BG = Path.of("src/main/resources/static/pjsk/box/background.png");
    public static final Path RENDER_BOARDER_BASE = Path.of("src/main/resources/static/pjsk/box/boarder/");
    public static final Path RENDER_RARITY_BIRTH = Path.of("src/main/resources/static/pjsk/box/rarity_birthday.png");
    public static final Path RENDER_RARITY_STAR = Path.of("src/main/resources/static/pjsk/box/star.png");
    public static final Path RENDER_ATTR = Path.of("src/main/resources/static/pjsk/box/attribute/");
    public static final Path RENDER_THUMBNAILS_BASE = Path.of("static/pjsk/master_data/cards/thumbnails/");

    public Path resolveMysekaiResourcePath(Path baseDir, String region, String id) throws IOException {
        // 400: 参数格式
        if (!region.matches("[a-zA-Z]+") || !id.matches("[0-9]+")) {
            throw new IllegalArgumentException("Invalid region or id format");
        }

        Path base = baseDir.normalize();
        Path file = base.resolve(region + "_" + id + ".png").normalize();

        // 403: 安全校验，防止目录穿越
        if (!file.startsWith(base)) {
            throw new SecurityException("Path traversal attempt detected");
        }

        // 404
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IOException("File not found");
        }

        return file;
    }

    public Path getSuitePath(String region, String id) {
        return PJSK_SUITE_JSON_BASE.resolve(region).resolve(String.format("%s.json", id));
    }

    public Path getCardBorderImgPath(PjskCard card) {
        return RENDER_BOARDER_BASE.resolve(String.format("%s.png", card.getRarities().name().toLowerCase()));
    }

    public Path getCardAttrImgPath(PjskCard card) {
        return RENDER_ATTR.resolve(String.format("%s.png", card.getAttributes().name().toLowerCase()));
    }

    public Path getThumbnailImgPath(String assetsbundleName, String specialTrainingStatus) {
        return RENDER_THUMBNAILS_BASE.resolve(String.format("%s_%s.png", assetsbundleName, specialTrainingStatus));
    }
}
