package com.arth.bot.plugins;

import com.arth.bot.adapter.sender.Sender;
import com.arth.bot.adapter.util.ImgExtractor;
import com.arth.bot.core.cache.service.CacheImageService;
import com.arth.bot.core.common.dto.ParsedPayloadDTO;
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
                          - speed <n> x: 加速 gif 为 n 倍
                            n 可以为负数，表示倒放
                          - gray: 转灰度图
                          - mirror: 水平镜像翻转
                          - gif: 转gif（QQ里看起来更像表情包）
                          - png: 转png""";

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

    public void speed(ParsedPayloadDTO payload, List<String> args) throws Exception {
        if (args == null || args.isEmpty()) {
            sender.replyText(payload, "没有指定加速 / 减速倍率哦");
            return;
        }

        String arg = args.get(0);
        if (arg.endsWith("x")) arg = arg.substring(0, arg.length() - 1);

        double rate;
        try { rate = Double.parseDouble(arg); }
        catch (NumberFormatException e) {
            sender.replyText(payload, "倍率参数不合法");
            return;
        }
        if (rate == 0) { sender.replyText(payload, "倍率不能为 0"); return; }

        boolean reverse = rate < 0;
        double r = Math.abs(rate);

        List<String> urls = imgExtractor.extractImgUrls(payload, true);
        if (urls == null || urls.isEmpty()) return;

        List<String> outUrls = new ArrayList<>();

        for (String url : urls) {
            if (!url.toLowerCase(Locale.ROOT).contains(".gif")) continue;

            byte[] inBytes = imgExtractor.getBytes(url);
            if (inBytes == null || inBytes.length == 0) continue;

            GifData src = readGif(new ByteArrayInputStream(inBytes));
            if (src.frames.isEmpty()) continue;

            GifData out = retime(src, r, reverse);
            byte[] outBytes = writeGifToBytes(out.frames, out.delaysCs, out.loopCount);

            String uuid = cacheImageService.cacheImage(outBytes);
            outUrls.add(baseUrl + "/cache/resource/imgs/gif/" + uuid);
        }

        if (!outUrls.isEmpty()) sender.sendImage(payload, outUrls);
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

    public void gif(ParsedPayloadDTO payload) throws IOException {
        toType(payload, "gif");
    }

    public void png(ParsedPayloadDTO payload) throws IOException {
        toType(payload, "png");
    }

    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****
    // ***** ============= helper ============= *****

    private static class GifData {
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delaysCs = new ArrayList<>();
        int loopCount = 0;
    }

    private static GifData readGif(InputStream input) throws IOException {
        GifData g = new GifData();
        try (ImageInputStream in = ImageIO.createImageInputStream(input)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) throw new IOException("No GIF reader");
            ImageReader reader = readers.next();
            reader.setInput(in, false);

            int num = reader.getNumImages(true);
            for (int i = 0; i < num; i++) {
                g.frames.add(reader.read(i));
                IIOMetadata meta = reader.getImageMetadata(i);
                g.delaysCs.add(readDelayCs(meta));
                if (i == 0) g.loopCount = readLoopCount(meta);
            }
            reader.dispose();
        }

        for (int i = 0; i < g.delaysCs.size(); i++) {
            if (g.delaysCs.get(i) <= 0) g.delaysCs.set(i, 10);
        }
        return g;
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
                gce.setAttribute("disposalMethod", "none");
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