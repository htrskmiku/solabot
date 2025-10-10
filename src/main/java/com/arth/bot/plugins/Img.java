package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.util.ImgExtractor;
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
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

@Component("plugins.img")
@BotPlugin({"img"})
@RequiredArgsConstructor
public class Img {

    private final Sender sender;
    private final ImgExtractor imgExtractor;
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
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImg(urls);

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

        List<String> uuids = cacheImageService.cacheImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/png/" + uuid);
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

        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImg(urls);

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

        List<String> uuids = cacheImageService.cacheImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/png/" + uuid);
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

        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;

        List<String> cacheUrls = new ArrayList<>();

        for (String url : urls) {
            byte[] inBytes = imgExtractor.getBytes(url);
            if (inBytes == null || inBytes.length == 0) continue;

            // GifData src = readGif(new ByteArrayInputStream(inBytes));
            GifData src = readGifFlattened(new ByteArrayInputStream(inBytes));
            if (src.frames.isEmpty()) continue;

            GifData out = retime(src, r, reverse);
            byte[] outBytes = writeGifToBytes(out.frames, out.delaysCs, out.loopCount);

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

        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImg(urls);

        // 强制使用支持 ARGB 的 BufferedImage 子类型，否则写入时 alpha 通道会被 ImageIO 忽略
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

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

        for (BufferedImage img : imgs) {
            int width = img.getWidth();
            int height = img.getHeight();
            /* map pixel to index */
            Map<Integer, Set<Long>> rgbSta = new HashMap<>();

            if (width < 6 || height < 6) {
                sender.replyText(payload, "图片过小");
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
                    rgbSta.computeIfAbsent(img.getRGB(width -i - 1, h), k -> new HashSet<>()).add(((long) (width -i - 1) << 32) | (h & 0xffffffffL));
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

        List<String> uuids = cacheImageService.cacheImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/png/" + uuid);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void gray(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImg(urls);

        for (int i = 0; i < imgs.size(); i++) {
            BufferedImage img = imgs.get(i);
            int width = img.getWidth();
            int height = img.getHeight();

            BufferedImage grayImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics g = grayImg.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            imgs.set(i, grayImg);
        }

        List<String> uuids = cacheImageService.cacheImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/png/" + uuid);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void mirror(ParsedPayloadDTO payload) throws IOException {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImg(urls);

        for (BufferedImage img : imgs) {
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

        List<String> uuids = cacheImageService.cacheImage(imgs);
        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/png/" + uuid);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }

    public void gif(ParsedPayloadDTO payload) throws IOException {
        toType(payload, "gif");
    }

    public void png(ParsedPayloadDTO payload) throws IOException {
        toType(payload, "png");
    }

    public void check(ParsedPayloadDTO payload) {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
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
    private static int distance(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;

        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = rgb2 & 0xff;

        return (r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2);
    }

    private static class GifData {
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delaysCs = new ArrayList<>();
        int loopCount = 0;
    }

//    private static GifData readGif(InputStream input) throws IOException {
//        GifData g = new GifData();
//        try (ImageInputStream in = ImageIO.createImageInputStream(input)) {
//            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
//            if (!readers.hasNext()) throw new IOException("No GIF reader");
//            ImageReader reader = readers.next();
//            reader.setInput(in, false);
//
//            int num = reader.getNumImages(true);
//            for (int i = 0; i < num; i++) {
//                g.frames.add(reader.read(i));
//                IIOMetadata meta = reader.getImageMetadata(i);
//                g.delaysCs.add(readDelayCs(meta));
//                if (i == 0) g.loopCount = readLoopCount(meta);
//            }
//            reader.dispose();
//        }
//
//        for (int i = 0; i < g.delaysCs.size(); i++) {
//            if (g.delaysCs.get(i) <= 0) g.delaysCs.set(i, 10);
//        }
//        return g;
//    }

    private static GifData readGifFlattened(InputStream input) throws IOException {
        GifData g = new GifData();
        try (ImageInputStream in = ImageIO.createImageInputStream(input)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) throw new IOException("No GIF reader");
            ImageReader reader = readers.next();
            reader.setInput(in, false);
            int num = reader.getNumImages(true);
            if (num <= 0) {
                reader.dispose();
                return g;
            }
            // ----------- 1) 取画布尺寸：先 stream metadata 的 LSD，兜底遍历帧，再兜底第0帧尺寸 -----------
            int canvasW = 0, canvasH = 0;
            IIOMetadata streamMeta = reader.getStreamMetadata();
            if (streamMeta != null) {
                try {
                    String sfmt = streamMeta.getNativeMetadataFormatName();
                    if (sfmt != null) {
                        IIOMetadataNode sroot = (IIOMetadataNode) streamMeta.getAsTree(sfmt);
                        IIOMetadataNode lsd = findNode(sroot, "LogicalScreenDescriptor");
                        if (lsd != null) {
                            canvasW = parseIntSafe(lsd, "logicalScreenWidth", 0);
                            canvasH = parseIntSafe(lsd, "logicalScreenHeight", 0);
                        }
                    }
                } catch (Exception ignore) {}
            }
            if (canvasW <= 0 || canvasH <= 0) {
                // 遍历每帧，基于 ImageDescriptor 的 (x,y,w,h) 求最大包围盒
                for (int i = 0; i < num; i++) {
                    try {
                        IIOMetadata im = reader.getImageMetadata(i);
                        String ifmt = im.getNativeMetadataFormatName();
                        IIOMetadataNode iroot = (IIOMetadataNode) im.getAsTree(ifmt);
                        IIOMetadataNode imgDesc = findNode(iroot, "ImageDescriptor");
                        int fx = 0, fy = 0, fw, fh;
                        if (imgDesc != null) {
                            fx = parseIntSafe(imgDesc, "imageLeftPosition", 0);
                            fy = parseIntSafe(imgDesc, "imageTopPosition", 0);
                            fw = parseIntSafe(imgDesc, "imageWidth", reader.getWidth(i));
                            fh = parseIntSafe(imgDesc, "imageHeight", reader.getHeight(i));
                        } else {
                            // 有些实现没有 ImageDescriptor，就退回帧图尺寸
                            fw = reader.getWidth(i);
                            fh = reader.getHeight(i);
                        }
                        canvasW = Math.max(canvasW, fx + fw);
                        canvasH = Math.max(canvasH, fy + fh);
                    } catch (Exception e) {
                        // continue
                    }
                }
            }
            if (canvasW <= 0) canvasW = reader.getWidth(0);
            if (canvasH <= 0) canvasH = reader.getHeight(0);
            // ----------- 2) 准备工作画布 -----------
            BufferedImage work = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D wg = work.createGraphics();
            wg.setComposite(AlphaComposite.SrcOver);
            // ----------- 3) 帧循环：按处置规则拍平为“独立全画布帧” -----------
            for (int i = 0; i < num; i++) {
                BufferedImage raw = reader.read(i);
                IIOMetadata im = reader.getImageMetadata(i);
                String ifmt = im.getNativeMetadataFormatName();
                IIOMetadataNode iroot = (IIOMetadataNode) im.getAsTree(ifmt);
                // 读取帧矩形
                IIOMetadataNode imgDesc = findNode(iroot, "ImageDescriptor");
                int fx = (imgDesc != null) ? parseIntSafe(imgDesc, "imageLeftPosition", 0) : 0;
                int fy = (imgDesc != null) ? parseIntSafe(imgDesc, "imageTopPosition", 0) : 0;
                int fw = (imgDesc != null) ? parseIntSafe(imgDesc, "imageWidth", raw.getWidth()) : raw.getWidth();
                int fh = (imgDesc != null) ? parseIntSafe(imgDesc, "imageHeight", raw.getHeight()) : raw.getHeight();
                // 读取处置
                IIOMetadataNode gce = findNode(iroot, "GraphicControlExtension");
                String disposal = (gce != null) ? gce.getAttribute("disposalMethod") : "none";
                if (disposal == null || disposal.isEmpty()) disposal = "none";
                // 在绘制当前帧之前保存快照（仅当本帧的处置是 restoreToPrevious 时需要）
                BufferedImage prevSnapshot = null;
                if ("restoreToPrevious".equals(disposal)) {
                    prevSnapshot = deepCopy(work);
                }
                // 把当前帧绘制到工作画布 (fx, fy)
                wg.drawImage(raw, fx, fy, null);
                // 生成独立全画布帧快照
                g.frames.add(deepCopy(work));
                // 记录延时和循环
                int delayCs = readDelayCs(im);
                g.delaysCs.add(delayCs > 0 ? delayCs : 10);
                if (i == 0) g.loopCount = readLoopCount(im);
                // 根据处置法处理工作画布
                if ("restoreToBackgroundColor".equals(disposal)) {
                    clearRectTransparent(work, fx, fy, fw, fh);
                } else if ("restoreToPrevious".equals(disposal) && prevSnapshot != null) {
                    Graphics2D g2 = work.createGraphics();
                    g2.setComposite(AlphaComposite.Src);
                    g2.drawImage(prevSnapshot, 0, 0, null);
                    g2.dispose();
                }
                // nothing to do for case none
            }
            wg.dispose();
            reader.dispose();
        }
        return g;
    }

    private static int parseIntSafe(IIOMetadataNode n, String attr, int defVal) {
        if (n == null) return defVal;
        try {
            String v = n.getAttribute(attr);
            if (v == null || v.isEmpty()) return defVal;
            return Integer.parseInt(v);
        } catch (Exception e) {
            return defVal;
        }
    }

    private static BufferedImage deepCopy(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return dst;
    }

    private static void clearRectTransparent(BufferedImage img, int x, int y, int w, int h) {
        Graphics2D g2 = img.createGraphics();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(x, y, w, h);
        g2.dispose();
    }

    private static int readDelayCs(IIOMetadata metadata) {
        try {
            String format = metadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            IIOMetadataNode gce = findNode(root, "GraphicControlExtension");
            if (gce != null) {
                String delay = gce.getAttribute("delayTime");
                return Integer.parseInt(delay);
            }
        } catch (Exception ignore) {}
        return 10;
    }

    private static int readLoopCount(IIOMetadata metadata) {
        try {
            String format = metadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            IIOMetadataNode aes = findNode(root, "ApplicationExtensions");
            if (aes != null) {
                for (int i = 0; i < aes.getLength(); i++) {
                    IIOMetadataNode ae = (IIOMetadataNode) aes.item(i);
                    if ("ApplicationExtension".equals(ae.getNodeName())) {
                        String appID = ae.getAttribute("applicationID");
                        String auth = ae.getAttribute("authenticationCode");
                        if ("NETSCAPE".equals(appID) && "2.0".equals(auth)) {
                            byte[] bytes = (byte[]) ae.getUserObject();
                            if (bytes != null && bytes.length >= 3) {
                                return ((bytes[2] & 0xFF) << 8) | (bytes[1] & 0xFF);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
        return 0;
    }

    private static IIOMetadataNode findNode(IIOMetadataNode root, String name) {
        if (root == null) return null;
        if (name.equals(root.getNodeName())) return root;
        for (int i = 0; i < root.getLength(); i++) {
            IIOMetadataNode res = findNode((IIOMetadataNode) root.item(i), name);
            if (res != null) return res;
        }
        return null;
    }

    private static byte[] writeGifToBytes(List<BufferedImage> frames, List<Integer> delaysCs, int loopCount) throws IOException {
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
                IIOMetadataNode gce = findNode(fmRoot, "GraphicControlExtension");
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

    private static GifData retime(GifData src, double rateAbs, boolean reverse) {
        if (rateAbs <= 0) throw new IllegalArgumentException("rate must be > 0");

        List<BufferedImage> frames = new ArrayList<>(src.frames);
        List<Integer> delays = new ArrayList<>(src.delaysCs);

        if (reverse) {
            Collections.reverse(frames);
            Collections.reverse(delays);
        }

        GifData out = new GifData();
        out.loopCount = src.loopCount;

        if (rateAbs == 1.0) {
            out.frames = frames;
            out.delaysCs = delays;
            return out;
        }

        if (rateAbs < 1.0) {
            for (int i = 0; i < frames.size(); i++) {
                int d = delays.get(i);
                int nd = Math.max(GIF_MIN_CS, Math.min(GIF_MAX_CS, (int)Math.round(d / rateAbs)));
                out.frames.add(frames.get(i));
                out.delaysCs.add(nd);
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
            out.frames.add(curF.get(i));
            out.delaysCs.add(scaled);
        }
        if (out.frames.isEmpty()) {
            out.frames.add(frames.get(0));
            out.delaysCs.add(Math.max(GIF_MIN_CS, (int)Math.round(delays.get(0) / rateAbs)));
        }
        return out;
    }

    public void toType(ParsedPayloadDTO payload, String type) throws IOException {
        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;
        List<BufferedImage> imgs = imgExtractor.getBufferedImg(urls);

        List<String> uuids = new ArrayList<>();
        for (BufferedImage img : imgs) {
            uuids.add(cacheImageService.cacheImage(img, type));
        }

        List<String> cacheUrls = new ArrayList<>(uuids.size());
        for (String uuid : uuids) cacheUrls.add(baseUrl + "/cache/resource/imgs/" + type + "/" + uuid);
        if (!cacheUrls.isEmpty()) sender.sendImage(payload, cacheUrls);
    }
}