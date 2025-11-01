package com.arth.bot.plugin.custom.pjsk.render;

import com.arth.bot.plugin.custom.pjsk.Pjsk;
import com.arth.bot.plugin.custom.pjsk.func.AssetsBundleResources;
import com.arth.bot.plugin.custom.pjsk.objects.PjskCard;
import com.arth.bot.plugin.custom.pjsk.objects.enums.CardAttributes;
import com.arth.bot.plugin.custom.pjsk.objects.enums.CardRarities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class ImageRenderer {
    public enum ExtendDirection {
        DOWN,
        RIGHT
    }

    //懒得学，ai不香吗
    private static BufferedImage mergeImages(BufferedImage background,
                                            BufferedImage foreground,
                                            int x, int y) {
        // 获取 Graphics2D 对象
        Graphics2D g2d = background.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // 绘制背景图
        g2d.drawImage(background, 0, 0, null);
        // 绘制前景图（自动处理Alpha通道）
        g2d.drawImage(foreground, x, y, null);
        g2d.dispose();
        return background;
    }

    /**
     * 将传入图片多次拼接以扩展图片分辨率
     * @param image
     * @param times
     * @param direction
     * @return
     */
    private static BufferedImage extendImage(BufferedImage image,int times,ExtendDirection direction) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int targetWidth;
        int targetHeight;
        switch (direction) {
            case RIGHT:
                targetWidth = image.getWidth() + image.getWidth()*times;
                targetHeight = image.getHeight();
                break;
            case DOWN:
                targetWidth = image.getWidth();
                targetHeight = image.getHeight() + image.getHeight()*times;
                break;
            default:
                targetWidth =  image.getWidth();
                targetHeight = image.getHeight();
                break;
        }

        BufferedImage combined = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = combined.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2d.drawImage(image, 0, 0, null);
        switch (direction) {
            case DOWN:
                for (int i = 0; i < times; i++) {
                    g2d.drawImage(image, 0 , (i+1)*image.getHeight(), null);
                }
                break;
            case RIGHT:
            default:
                for (int i = 0; i < times; i++) {
                    g2d.drawImage(image, imageWidth + i*imageWidth, 0, null);
                }
                break;

        }

        g2d.dispose();
        return combined;
    }


//    private static void generateSaveFile(BufferedImage buffImg, String savePath) {
//        int temp = savePath.lastIndexOf(".") + 1;
//        try {
//            File outFile = new File(savePath);
//            if(!outFile.exists()){
//                outFile.createNewFile();
//            }
//            ImageIO.write(buffImg, savePath.substring(temp), outFile);
//            System.out.println("ImageIO write...");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 压缩 BufferedImage 为 PNG 格式
     * @param image 原始图像
     * @param compressionQuality 压缩质量 (0.0-1.0)
     * @return 压缩后的 BufferedImage
     */
    public static BufferedImage compressPNG(BufferedImage image, float compressionQuality) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            if (!writers.hasNext()) {
                return image; // 无法压缩，返回原图
            }

            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();

            // 如果可以设置压缩模式，则设置
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(compressionQuality);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);

            ios.close();
            writer.dispose();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return ImageIO.read(bais);

        } catch (IOException e) {
            System.err.println("PNG compression failed: " + e.getMessage());
            return image;
        }
    }

    /**
     * 缩放图片
     * @param originalImage
     * @param width
     * @param height
     * @return
     */
    //卡面缩略图原大小为128x128，需要缩放至140x140
    private static BufferedImage resize(BufferedImage originalImage,int width,int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();
        return resizedImage;
    }


    //卡片渲染，大小固定为156*156
    public static class Card{
        private CardAttributes attributes;
        private CardRarities rarities;
        private BufferedImage thumbnails;
        private BufferedImage boarder;
        private BufferedImage rarityImage;
        private BufferedImage attributeImage;
        private BufferedImage output;
        private Pjsk.BeanContext ctx;
        private boolean shouldDrawRarity = true;
        private boolean shouldDrawBoarder = true;
        private boolean shouldDrawAttribute = true;
        //https://assets.unipjsk.com/startapp/thumbnail/chara/{assetbundle_name}_{status}.png
        public Card(Pjsk.BeanContext ctx,PjskCard pjskCard) throws IOException {
            this.ctx = ctx;
            attributes = pjskCard.getAttributes();
            rarities = pjskCard.getRarities();
            thumbnails = AssetsBundleResources.getOrCacheThumbnailByCard(ctx,pjskCard);
            
            boarder = ImageIO.read(ResourceUtils.getFile("src/main/resources/static/pjsk/box/boarder/" + pjskCard.getRarities().name().toLowerCase() + ".png"));
            rarityImage = ImageIO.read(ResourceUtils.getFile("src/main/resources/static/pjsk/box/" +
                    (pjskCard.getRarities().equals(CardRarities.RARITY_BIRTHDAY) ? "rarity_birthday.png" : "star.png")));
            attributeImage = ImageIO.read(ResourceUtils.getFile("src/main/resources/static/pjsk/box/attribute/" + pjskCard.getAttributes().name().toLowerCase() + ".png"));
        }
        public void noDrawRarity(){shouldDrawRarity = false;}
        public void noDrawBoarder(){shouldDrawBoarder = false;}
        public void noDrawAttribute(){shouldDrawAttribute = false;}
        public BufferedImage draw(){
            rarityImage = resize(rarityImage,25,25);
            attributeImage = resize(attributeImage,30,30);//缩小稀有度与属性标识
            thumbnails = resize(thumbnails,140,140);
            if(shouldDrawBoarder){
                if (rarities.equals(CardRarities.RARITY_BIRTHDAY)){
                    BufferedImage bufferedBoarder = ctx.imgService().deepCopy(boarder);//TODO：优化流程
                    output = mergeImages(boarder,thumbnails,8,8);//基本框+缩略图
                    mergeImages(output,bufferedBoarder,0,0);//再覆盖一次框，生日卡的框比较特殊
                }else {
                    output = mergeImages(boarder,thumbnails,8,8);//基本框+缩略图
                }
                //TODO:优化绘图流程，这里是史山
            }else{
                output = thumbnails;
            }
            if (shouldDrawRarity) {
                for (int i = 0; i < rarities.getDrawQuantity(); i++) {
                    mergeImages(output, rarityImage, 9 + 25 * i, 124);//画稀有度
                }
            }
            if (shouldDrawAttribute) {
                mergeImages(output,attributeImage,8,8);
            }

            return output;
        }
    }


    //输出渲染box,单个背景最多可放60张卡(5x10)
    public static class Box{
        public enum BoxDrawMethod{
            RARITIES_IN_DESCEND,
            CHARA_ID_IN_ASCEND;
        }
        private ArrayList<PjskCard> cards;
        private BufferedImage background;
        private BufferedImage output;
        public Box(ArrayList<PjskCard> cards) throws IOException {
            this.background = ImageIO.read(ResourceUtils.getFile("src/main/resources/static/pjsk/box/background.png"));
            this.output = background;
            this.cards = cards;
        }


        /**
         * 根据稀有度降序
         * @return
         */
        private BufferedImage drawByCharities(){
            //纵向排列
            int extendTimes = cards.size() / 60;//需要扩展几次背景
            output = extendImage(background,extendTimes,ExtendDirection.RIGHT);
            int singleColumnDrawn = 0;
            int columns = 0;
            for (PjskCard card : cards) {//慢死了
                if (singleColumnDrawn >= 10) {
                    columns++;
                    singleColumnDrawn = 0;
                }
                mergeImages(output,card.getThumbnails(),16 + columns * 240,26 +  singleColumnDrawn * 233);
                singleColumnDrawn++;
            }
            //generateSaveFile(output,"static/out.png");
            output = resize(output, output.getWidth()/2, output.getHeight()/2);
            return output;
        }

        /**
         * 根据角色ID排序
         * @return
         */
        private BufferedImage drawByCharacterId(){
            //output = extendImage(background,4,ExtendDirection.RIGHT);
            cards.sort(Comparator.comparingInt(a -> a.getCardCharacters().getCharaId()));
            cards.sort((a,b)->{
                if (a.getCardCharacters().getCharaId() == b.getCardCharacters().getCharaId()) {
                    return -Integer.compare(a.getRarities().getRarity(),b.getRarities().getRarity());
                }
                return 0;
            });
            //首先按角色ID升序，再按稀有度降序
            int[] countOfEachCharacter = new int[26];//每个角色的卡数
            for (PjskCard pjskCard : cards) {
                countOfEachCharacter[pjskCard.getCardCharacters().getCharaId()-1]++;
            }
            Arrays.sort(countOfEachCharacter);
            int maxCardCount = countOfEachCharacter[25];//选取最多的一项，这里可以优化吗？不懂
            int extendPages = maxCardCount/10;
            output = resize(background, 6500, background.getHeight() * (extendPages+1));
            //output = extendImage(output,extendPages,ExtendDirection.DOWN);
            short drawColumn = 0;
            short drawRow = 0;
            for(int i = 0; i < cards.size(); i++){
                mergeImages(output,cards.get(i).getThumbnails(),16 + drawColumn * 240,drawRow * 233);
                drawRow++;
                if(i+1 != cards.size() && cards.get(i).getCardCharacters() != cards.get(i+1).getCardCharacters()){
                    drawColumn++;
                    drawRow = 0;
                }
            }
            output = resize(output,output.getWidth()/2,output.getHeight()/2);
            return output;
        }

        /**
         * 开始绘制，阻塞时间较长
         * @return
         */
        public BufferedImage draw(BoxDrawMethod drawMethod){
            long startMs = System.currentTimeMillis();
            BufferedImage image;
            switch (drawMethod){
                case RARITIES_IN_DESCEND:
                    image = drawByCharities();
                    break;
                case CHARA_ID_IN_ASCEND:
                default:
                    image = drawByCharacterId();
            }

            long stopMs = System.currentTimeMillis();
            log.info("Drawing box completed.Used {} ms.",stopMs-startMs);
            return image;
        }


    }

}
