package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.util.ImgService;
import com.arth.bot.adapter.util.ImgService.GifData;
import com.arth.bot.core.cache.service.CacheImageService;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.invoker.annotation.BotPlugin;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Component("plugins.img")
@BotPlugin({"img"})
@RequiredArgsConstructor
public class Img {

    private final Sender sender;
    private final ImgService imgService;
    private final CacheImageService cacheImageService;

    private static final int GIF_MIN_CS = 2;      // GIF 最小播放时间间隔
    private static final int GIF_MAX_CS = 65535;  // GIF 最大播放时间间隔

    @Value("${server.port}")
    private String port;

    @Value("${app.client-access-url}")
    private String clientAccessUrl;

    private String baseUrl;

    public static final String helpText = """
                        img 图片处理模块目前支持以下命令：
                          - mid: 镜像对称，默认左对称
                          - mid r: 镜像对称，右对称
                          - speed <n>: 加速 gif 为 n 倍
                            n 可以为负数，表示倒放
                          - cutout: 纯色背景抠图透明底
                            实现方案是自边缘背景起优先队列+DFS泛洪
                            可以跟 <阈值> 参数指定阈值，默认100
                            阈值为与背景RGB欧氏距离允许的均方误差
                          - gray: 转灰度图
                          - mirror: 水平镜像翻转
                          - gif: 转gif
                            在QQ里看起来会更像表情包
                          - png: 转png
                          - check: 检查图片url""";

    @PostConstruct
    public void init() {
        this.baseUrl = "http://" + clientAccessUrl + ":" + port;
    }

    public void index(ParsedPayloadDTO payload) {
        sender.sendText(payload, "具体的图片处理命令是什么呢？");
    }

    public void help(ParsedPayloadDTO payload) {
        sender.replyText(payload, helpText);
    }

    public void mid(ParsedPayloadDTO payload) throws IOException {
        // 支持静态图片与 GIF（动态）混合输入，输出保持输入顺序
        List<String> urls = imgService.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;

        int n = urls.size();
        List<String> cacheUrlsOrdered = new ArrayList<>(Collections.nCopies(n, null));

        for (int i = 0; i < n; i++) {
            String url = urls.get(i);
            byte[] data = imgService.getBytes(url);
            if (data == null || data.length == 0) continue;
            String type = imgService.detectImageType(data);
            if ("gif".equals(type)) {
                // 处理 GIF：解析、逐帧 midLeft、写回 GIF
                GifData gif = imgService.getGifFlattened(new ByteArrayInputStream(data));
                if (gif == null) continue;
                gifsMidLeft(List.of(gif));
                byte[] outBytes = writeGifToBytes(gif.getFrames(), gif.getDelaysCs(), gif.getLoopCount());
                String uuid = cacheImageService.cacheImage(outBytes);
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/gif/" + uuid);
            } else {
                // 处理静态图片（PNG/JPEG/...）
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img == null) continue;
                List<BufferedImage> imgs = new ArrayList<>();
                imgs.add(img);
                // 原有静态逻辑：midLeft
                midLeft(imgs);
                // 缓存为 png
                String uuid = cacheImageService.cacheImage(imgs.get(0), "png");
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/png/" + uuid);
            }
        }

        List<String> cacheUrls = new ArrayList<>();
        for (String s : cacheUrlsOrdered) if (s != null) cacheUrls.add(s);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void mid(ParsedPayloadDTO payload, List<String> args) throws IOException {
        if (args == null || args.isEmpty() || args.get(0).equals("l")  || args.get(0).equals("left")) {
            mid(payload);
            return;
        }

        if (!args.get(0).equals("r") && !args.get(0).equals("right")) {
            sender.replyText(payload, "mid 命令支持的参数是 l 或 r，默认 l");
            return;
        }

        // 右对称处理，支持静态与 GIF 混合
        List<String> urls = imgService.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;

        int n = urls.size();
        List<String> cacheUrlsOrdered = new ArrayList<>(Collections.nCopies(n, null));

        for (int i = 0; i < n; i++) {
            String url = urls.get(i);
            byte[] data = imgService.getBytes(url);
            if (data == null || data.length == 0) continue;
            String type = imgService.detectImageType(data);
            if ("gif".equals(type)) {
                GifData gif = imgService.getGifFlattened(new ByteArrayInputStream(data));
                if (gif == null) continue;
                gifsMidRight(List.of(gif));
                byte[] outBytes = writeGifToBytes(gif.getFrames(), gif.getDelaysCs(), gif.getLoopCount());
                String uuid = cacheImageService.cacheImage(outBytes);
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/gif/" + uuid);
            } else {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img == null) continue;
                List<BufferedImage> imgs = new ArrayList<>();
                imgs.add(img);
                midRight(imgs);
                String uuid = cacheImageService.cacheImage(imgs.get(0), "png");
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/png/" + uuid);
            }
        }

        List<String> cacheUrls = new ArrayList<>();
        for (String s : cacheUrlsOrdered) if (s != null) cacheUrls.add(s);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void speed(ParsedPayloadDTO payload, List<String> args) throws IOException {
        if (args == null || args.isEmpty()) {
            sender.replyText(payload, "没有指定加速 / 减速倍率哦");
            return;
        }

        String arg = args.get(0);
        if (arg.endsWith("x")) arg = arg.substring(0, arg.length() - 1);

        double rate;
        try {
            rate = Double.parseDouble(arg);
        }
        catch (NumberFormatException e) {
            sender.replyText(payload, "倍率参数不合法");
            return;
        }
        if (rate == 0) {
            sender.replyText(payload, "倍率不能为 0");
            return;
        }

        boolean reverse = rate < 0;
        double r = Math.abs(rate);

        List<String> urls = imgService.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;

        List<String> cacheUrls = new ArrayList<>();

        var gifs = imgService.getGifFlattened(urls);

        for (var gif : gifs) {
            GifData out = retime(gif, r, reverse);
            byte[] outBytes = writeGifToBytes(out.getFrames(), out.getDelaysCs(), out.getLoopCount());
            String uuid = cacheImageService.cacheImage(outBytes);
            cacheUrls.add(baseUrl + "/cache/resource/imgs/gif/" + uuid);
        }

        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void cutout(ParsedPayloadDTO payload, List<String> args) throws IOException {
        int threshold = 100;

        if (args != null && !args.isEmpty()) {
            try {
                threshold = Integer.parseInt(args.get(0));
            } catch (NumberFormatException e) {
                sender.replyText(payload, "阈值参数不合法");
                return;
            }
        }

        List<String> urls = imgService.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;

        // 支持静态图片与 GIF 的混合输入，输出顺序保留输入顺序
        int n = urls.size();
        List<String> cacheUrlsOrdered = new ArrayList<>(Collections.nCopies(n, null));

        for (int i = 0; i < n; i++) {
            String url = urls.get(i);
            byte[] data = imgService.getBytes(url);
            if (data == null || data.length == 0) continue;
            String type = imgService.detectImageType(data);
            if ("gif".equals(type)) {
                // GIF：逐帧抠图
                GifData gif = imgService.getGifFlattened(new ByteArrayInputStream(data));
                if (gif == null) continue;
                // 检查帧尺寸，若有帧过小则直接提示并返回（与静态逻辑保持一致）
                for (BufferedImage frame : gif.getFrames()) {
                    if (frame.getWidth() < 6 || frame.getHeight() < 6) {
                        sender.replyText(payload, "图片过小");
                        return;
                    }
                }
                gifsCutout(gif, threshold);
                byte[] outBytes = writeGifToBytes(gif.getFrames(), gif.getDelaysCs(), gif.getLoopCount());
                String uuid = cacheImageService.cacheImage(outBytes);
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/gif/" + uuid);
            } else {
                // 静态图片：保留原有完整逻辑（针对单张图片）
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img == null) continue;
                // 强制使用支持 ARGB 的 BufferedImage 子类型，否则写入时 alpha 通道会被 ImageIO 忽略
                List<BufferedImage> imgs = new ArrayList<>();
                imgs.add(img);
                toARGB(imgs);

                int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

                for (BufferedImage targetImg : imgs) {
                    int width = targetImg.getWidth();
                    int height = targetImg.getHeight();
                    /* map pixel to index */
                    Map<Integer, Set<Long>> rgbSta = new HashMap<>();

                    if (width < 6 || height < 6) {
                        sender.replyText(payload, "图片过小");
                        return;
                    }

                    // 1. 统计四个边缘上出现频次最高的颜色，每条边宽度硬编码 3
                    for (int w = 0; w < width; w++) {
                        for (int k = 0; k < 3; k++) {
                            rgbSta.computeIfAbsent(targetImg.getRGB(w, k), kk -> new HashSet<>()).add(((long) w << 32) | (k & 0xffffffffL));
                            rgbSta.computeIfAbsent(targetImg.getRGB(w, height - k - 1), kk -> new HashSet<>()).add(((long) w << 32) | ((height - k - 1) & 0xffffffffL));
                        }
                    }
                    for (int h = 1; h < height - 1; h++) {
                        for (int k = 0; k < 3; k++) {
                            rgbSta.computeIfAbsent(targetImg.getRGB(k, h), kk -> new HashSet<>()).add(((long) k << 32) | (h & 0xffffffffL));
                            rgbSta.computeIfAbsent(targetImg.getRGB(width - k - 1, h), kk -> new HashSet<>()).add(((long) (width - k - 1) << 32) | (h & 0xffffffffL));
                        }
                    }

                    Set<Long> bgPixelIndex = rgbSta.values().stream()
                            .max(Comparator.comparingInt(Set::size))
                            .orElseThrow(() -> new InternalServerErrorException("rgbSta cannot be empty"));

                    if (bgPixelIndex.isEmpty()) throw new InternalServerErrorException("bgPixelIndex cannot be empty");

                    long aBgIndex = bgPixelIndex.iterator().next();
                    int bgRgb = targetImg.getRGB((int) (aBgIndex >>> 32), (int) aBgIndex);

                    // 2. 初始化优先队列，将所有边缘上出现频次最高的颜色加入优先队列
                    Set<Long> visited = new HashSet<>(bgPixelIndex);
                    PriorityQueue<Pixel> pq = new PriorityQueue<>(Comparator.comparingDouble(Pixel::dist));
                    for (long index : bgPixelIndex) {
                        int x = (int) (index >>> 32);
                        int y = (int) index;
                        pq.offer(new Pixel(x, y, targetImg.getRGB(x, y), 0));
                    }

                    // 3. BFS 抠图透明底
                    while (!pq.isEmpty()) {
                        Pixel p = pq.poll();
                        int x = p.x();
                        int y = p.y();

                        if (p.dist() > threshold) continue;
                        targetImg.setRGB(x, y, p.rgb() & 0x00ffffff);

                        for (int[] d : dirs) {
                            int nx = x + d[0];
                            int ny = y + d[1];
                            if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;

                            long idx = ((long) nx << 32) | (ny & 0xffffffffL);
                            if (visited.contains(idx)) continue;

                            visited.add(idx);
                            int nRgb = targetImg.getRGB(nx, ny);
                            int dist = distance(bgRgb, nRgb);

                            pq.offer(new Pixel(nx, ny, nRgb, dist));
                        }
                    }
                }

                String uuid = cacheImageService.cacheImage(imgs.get(0), "png");
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/png/" + uuid);
            }
        }

        List<String> cacheUrls = new ArrayList<>();
        for (String s : cacheUrlsOrdered) if (s != null) cacheUrls.add(s);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void gray(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgService.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;

        int n = urls.size();
        List<String> cacheUrlsOrdered = new ArrayList<>(Collections.nCopies(n, null));

        for (int i = 0; i < n; i++) {
            String url = urls.get(i);
            byte[] data = imgService.getBytes(url);
            if (data == null || data.length == 0) continue;
            String type = imgService.detectImageType(data);
            if ("gif".equals(type)) {
                GifData gif = imgService.getGifFlattened(new ByteArrayInputStream(data));
                if (gif == null) continue;
                gifsGray(gif);
                byte[] outBytes = writeGifToBytes(gif.getFrames(), gif.getDelaysCs(), gif.getLoopCount());
                String uuid = cacheImageService.cacheImage(outBytes);
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/gif/" + uuid);
            } else {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img == null) continue;
                int width = img.getWidth();
                int height = img.getHeight();

                BufferedImage grayImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                Graphics g = grayImg.getGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                // 为与原本输出为 png 的路径一致，缓存为 ARGB png
                BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = outImg.createGraphics();
                g2.drawImage(grayImg, 0, 0, null);
                g2.dispose();

                String uuid = cacheImageService.cacheImage(outImg, "png");
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/png/" + uuid);
            }
        }

        List<String> cacheUrls = new ArrayList<>();
        for (String s : cacheUrlsOrdered) if (s != null) cacheUrls.add(s);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void mirror(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgService.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;

        int n = urls.size();
        List<String> cacheUrlsOrdered = new ArrayList<>(Collections.nCopies(n, null));

        for (int i = 0; i < n; i++) {
            String url = urls.get(i);
            byte[] data = imgService.getBytes(url);
            if (data == null || data.length == 0) continue;
            String type = imgService.detectImageType(data);
            if ("gif".equals(type)) {
                GifData gif = imgService.getGifFlattened(new ByteArrayInputStream(data));
                if (gif == null) continue;
                gifsMirror(gif);
                byte[] outBytes = writeGifToBytes(gif.getFrames(), gif.getDelaysCs(), gif.getLoopCount());
                String uuid = cacheImageService.cacheImage(outBytes);
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/gif/" + uuid);
            } else {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img == null) continue;
                int width = img.getWidth();
                int height = img.getHeight();

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width / 2; x++) {
                        int leftPixel = img.getRGB(x, y);
                        int rightPixel = img.getRGB(width - x - 1, y);

                        img.setRGB(x, y, rightPixel);
                        img.setRGB(width - x - 1, y, leftPixel);
                    }
                }

                String uuid = cacheImageService.cacheImage(img, "png");
                cacheUrlsOrdered.set(i, baseUrl + "/cache/resource/imgs/png/" + uuid);
            }
        }

        List<String> cacheUrls = new ArrayList<>();
        for (String s : cacheUrlsOrdered) if (s != null) cacheUrls.add(s);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void gif(ParsedPayloadDTO payload) throws IOException {
        toType(payload, "gif");
    }

    public void png(ParsedPayloadDTO payload) throws IOException {
        toType(payload, "png");
    }

    public void check(ParsedPayloadDTO payload) {
        List<String> urls = imgService.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) {
            sender.sendText(payload, "引用消息里没有找到图片");
        } else {
            sender.replyText(payload, "提取到的图片 URL 为：" + urls);
        }
    }

    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****

    private record Pixel(int x, int y, int rgb, int dist) {}

    /**
     * 返回 RGB 的平方距离差值（避免欧氏距离的根号计算）
     * @param rgb1
     * @param rgb2
     * @return
     */
    private int distance(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;

        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = rgb2 & 0xff;

        return (r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2);
    }

    private void toARGB(List<BufferedImage> imgs) {
        for (int i = 0; i < imgs.size(); i++) {
            BufferedImage img = imgs.get(i);
            if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                BufferedImage argbImg = new BufferedImage(
                        img.getWidth(),
                        img.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g = argbImg.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                imgs.set(i, argbImg);
            }
        }
    }

    private byte[] writeGifToBytes(List<BufferedImage> frames, List<Integer> delaysCs, int loopCount) throws IOException {
        if (frames == null || frames.isEmpty()) throw new IllegalArgumentException("No frames");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ImageWriteParam params = writer.getDefaultWriteParam();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);

            // 流级元数据：写入 NETSCAPE 2.0 循环扩展
            IIOMetadata streamMeta = writer.getDefaultImageMetadata(type, params);
            String format = streamMeta.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) streamMeta.getAsTree(format);
            IIOMetadataNode appExts = new IIOMetadataNode("ApplicationExtensions");
            IIOMetadataNode appExt = new IIOMetadataNode("ApplicationExtension");
            appExt.setAttribute("applicationID", "NETSCAPE");
            appExt.setAttribute("authenticationCode", "2.0");
            int loop = Math.max(0, loopCount); // 0 = 无限
            byte[] loopBytes = new byte[] { 0x01, (byte)(loop & 0xFF), (byte)((loop >> 8) & 0xFF) };
            appExt.setUserObject(loopBytes);
            appExts.appendChild(appExt);
            root.appendChild(appExts);
            streamMeta.setFromTree(format, root);

            writer.prepareWriteSequence(streamMeta);

            for (int i = 0; i < frames.size(); i++) {
                BufferedImage bi = frames.get(i);
                int delay = Math.min(Math.max(delaysCs.get(i), GIF_MIN_CS), GIF_MAX_CS);

                IIOMetadata frameMeta = writer.getDefaultImageMetadata(type, params);
                IIOMetadataNode fmRoot = (IIOMetadataNode) frameMeta.getAsTree(format);
                IIOMetadataNode gce = imgService.findNode(fmRoot, "GraphicControlExtension");
                if (gce == null) {
                    gce = new IIOMetadataNode("GraphicControlExtension");
                    fmRoot.appendChild(gce);
                }
                gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
                gce.setAttribute("userInputFlag", "FALSE");
                gce.setAttribute("transparentColorFlag", "FALSE");
                gce.setAttribute("delayTime", Integer.toString(delay));
                gce.setAttribute("transparentColorIndex", "0");

                frameMeta.setFromTree(format, fmRoot);
                writer.writeToSequence(new IIOImage(bi, null, frameMeta), params);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private GifData retime(GifData gif, double rateAbs, boolean reverse) {
        if (rateAbs <= 0) throw new IllegalArgumentException("rate must be > 0");

        List<BufferedImage> frames = new ArrayList<>(gif.getFrames());
        List<Integer> delays = new ArrayList<>(gif.getDelaysCs());

        if (reverse) {
            Collections.reverse(frames);
            Collections.reverse(delays);
        }

        GifData out = new GifData();
        out.setLoopCount(gif.getLoopCount());

        if (rateAbs == 1.0) {
            out.setFrames(frames);
            out.setDelaysCs(delays);
            return out;
        }

        if (rateAbs < 1.0) {
            for (int i = 0; i < frames.size(); i++) {
                int d = delays.get(i);
                int nd = Math.max(GIF_MIN_CS, Math.min(GIF_MAX_CS, (int)Math.round(d / rateAbs)));
                out.getFrames().add(frames.get(i));
                out.getDelaysCs().add(nd);
            }
            return out;
        }

        double need = rateAbs;
        List<BufferedImage> curF = frames;
        List<Integer> curD = delays;

        while (true) {
            boolean allOk = true;
            for (int d : curD) {
                int scaled = Math.max((int)Math.round(d / need), GIF_MIN_CS);
                if (scaled <= GIF_MIN_CS) {
                    allOk = false;
                    break;
                }
            }
            if (allOk) break;

            // 当无法保证最小播放间隔时间处于合理范围时，考虑删帧
            // 删奇数帧（索引 1,3,5, ...），保留偶数 0,2,4,...
            if (curF.size() <= 1) break;
            List<BufferedImage> nf = new ArrayList<>((curF.size()+1)/2);
            List<Integer> nd = new ArrayList<>((curD.size()+1)/2);
            for (int i = 0; i < curF.size(); i += 2) {
                nf.add(curF.get(i));
                nd.add(curD.get(i));
            }
            curF = nf;
            curD = nd;

            need /= 2.0;
            if (need <= 1.0) break;
        }

        for (int i = 0; i < curF.size(); i++) {
            int d = curD.get(i);
            int scaled = Math.max((int)Math.round(d / Math.max(need, 1.0)), GIF_MIN_CS);
            out.getFrames().add(curF.get(i));
            out.getDelaysCs().add(scaled);
        }
        if (out.getFrames().isEmpty()) {
            out.getFrames().add(frames.get(0));
            out.getDelaysCs().add(Math.max(GIF_MIN_CS, (int)Math.round(delays.get(0) / rateAbs)));
        }
        return out;
    }

    private void midLeft(List<BufferedImage> imgs) {
        for (BufferedImage img : imgs) {
            int width = img.getWidth();
            int height = img.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width / 2; x++) {
                    int leftPixel = img.getRGB(x, y);
                    img.setRGB(width - x - 1, y, leftPixel);
                }
            }
        }
    }

    public void midRight(List<BufferedImage> imgs) {
        for (BufferedImage img : imgs) {
            int width = img.getWidth();
            int height = img.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width / 2; x++) {
                    int rightPixel = img.getRGB(width - x - 1, y);
                    img.setRGB(x, y, rightPixel);
                }
            }
        }
    }

    private void gifsMidLeft(List<GifData> gifs) {
        for (GifData g : gifs) {
            List<BufferedImage> frames = g.getFrames();
            for (BufferedImage img : frames) {
                // 保证 ARGB
                if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                    BufferedImage argb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gg = argb.createGraphics();
                    gg.drawImage(img, 0, 0, null);
                    gg.dispose();
                    // replace contents
                    Graphics2D g2 = img.createGraphics();
                    g2.setComposite(AlphaComposite.Src);
                    g2.drawImage(argb, 0, 0, null);
                    g2.dispose();
                }
                int width = img.getWidth();
                int height = img.getHeight();

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width / 2; x++) {
                        int leftPixel = img.getRGB(x, y);
                        img.setRGB(width - x - 1, y, leftPixel);
                    }
                }
            }
        }
    }

    public void gifsMidRight(List<GifData> gifs) {
        for (GifData g : gifs) {
            List<BufferedImage> frames = g.getFrames();
            for (BufferedImage img : frames) {
                if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                    BufferedImage argb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gg = argb.createGraphics();
                    gg.drawImage(img, 0, 0, null);
                    gg.dispose();
                    Graphics2D g2 = img.createGraphics();
                    g2.setComposite(AlphaComposite.Src);
                    g2.drawImage(argb, 0, 0, null);
                    g2.dispose();
                }
                int width = img.getWidth();
                int height = img.getHeight();

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width / 2; x++) {
                        int rightPixel = img.getRGB(width - x - 1, y);
                        img.setRGB(x, y, rightPixel);
                    }
                }
            }
        }
    }

    private void gifsCutout(GifData gif, int threshold) {
        // 对每一帧单独执行 cutout 算法（与静态图片的处理逻辑对齐）
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        List<BufferedImage> frames = gif.getFrames();
        for (int fi = 0; fi < frames.size(); fi++) {
            BufferedImage img = frames.get(fi);
            // 强制 ARGB
            if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                BufferedImage argbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = argbImg.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                frames.set(fi, argbImg);
                img = argbImg;
            }

            int width = img.getWidth();
            int height = img.getHeight();
            Map<Integer, Set<Long>> rgbSta = new HashMap<>();

            if (width < 6 || height < 6) {
                // 图片过小
                return;
            }

            // 1. 统计四个边缘上出现频次最高的颜色，每条边宽度硬编码 3
            for (int w = 0; w < width; w++) {
                for (int i = 0; i < 3; i++) {
                    rgbSta.computeIfAbsent(img.getRGB(w, i), k -> new HashSet<>()).add(((long) w << 32) | (i & 0xffffffffL));
                    rgbSta.computeIfAbsent(img.getRGB(w, height - i - 1), k -> new HashSet<>()).add(((long) w << 32) | ((height - i - 1) & 0xffffffffL));
                }
            }
            for (int h = 1; h < height - 1; h++) {
                for (int i = 0; i < 3; i++) {
                    rgbSta.computeIfAbsent(img.getRGB(i, h), k -> new HashSet<>()).add(((long) i << 32) | (h & 0xffffffffL));
                    rgbSta.computeIfAbsent(img.getRGB(width - i - 1, h), k -> new HashSet<>()).add(((long) (width - i - 1) << 32) | (h & 0xffffffffL));
                }
            }

            Set<Long> bgPixelIndex = rgbSta.values().stream()
                    .max(Comparator.comparingInt(Set::size))
                    .orElseThrow(() -> new InternalServerErrorException("rgbSta cannot be empty"));

            if (bgPixelIndex.isEmpty()) throw new InternalServerErrorException("bgPixelIndex cannot be empty");

            long aBgIndex = bgPixelIndex.iterator().next();
            int bgRgb = img.getRGB((int) (aBgIndex >>> 32), (int) aBgIndex);

            // 2. 初始化优先队列，将所有边缘上出现频次最高的颜色加入优先队列
            Set<Long> visited = new HashSet<>(bgPixelIndex);
            PriorityQueue<Pixel> pq = new PriorityQueue<>(Comparator.comparingDouble(Pixel::dist));
            for (long index : bgPixelIndex) {
                int x = (int) (index >>> 32);
                int y = (int) index;
                pq.offer(new Pixel(x, y, img.getRGB(x, y), 0));
            }

            // 3. BFS 抠图透明底
            while (!pq.isEmpty()) {
                Pixel p = pq.poll();
                int x = p.x();
                int y = p.y();

                if (p.dist() > threshold) continue;
                img.setRGB(x, y, p.rgb() & 0x00ffffff);

                for (int[] d : dirs) {
                    int nx = x + d[0];
                    int ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;

                    long idx = ((long) nx << 32) | (ny & 0xffffffffL);
                    if (visited.contains(idx)) continue;

                    visited.add(idx);
                    int nRgb = img.getRGB(nx, ny);
                    int dist = distance(bgRgb, nRgb);

                    pq.offer(new Pixel(nx, ny, nRgb, dist));
                }
            }
        }
    }

    private void gifsGray(GifData gif) {
        List<BufferedImage> frames = gif.getFrames();
        for (int i = 0; i < frames.size(); i++) {
            BufferedImage img = frames.get(i);
            int width = img.getWidth();
            int height = img.getHeight();

            BufferedImage grayImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics g = grayImg.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();

            // 转回 ARGB（writeGifToBytes 期望 ARGB）
            BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = outImg.createGraphics();
            g2.drawImage(grayImg, 0, 0, null);
            g2.dispose();

            frames.set(i, outImg);
        }
    }

    private void gifsMirror(GifData gif) {
        List<BufferedImage> frames = gif.getFrames();
        for (BufferedImage img : frames) {
            if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                BufferedImage argbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = argbImg.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                img = argbImg;
            }
            int width = img.getWidth();
            int height = img.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width / 2; x++) {
                    int leftPixel = img.getRGB(x, y);
                    int rightPixel = img.getRGB(width - x - 1, y);

                    img.setRGB(x, y, rightPixel);
                    img.setRGB(width - x - 1, y, leftPixel);
                }
            }
        }
    }

    public void toType(ParsedPayloadDTO payload, String type) throws IOException {
        List<String> urls = imgService.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgService.getBufferedImg(urls);

        List<String> uuids = new ArrayList<>();
        for (BufferedImage img : imgs) {
            uuids.add(cacheImageService.cacheImage(img, type));
        }

        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/" + type + "/" + uuid);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }
}
